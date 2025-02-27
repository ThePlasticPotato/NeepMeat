package com.neep.neepmeat.api.live_machine;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.api.live_machine.metrics.DataLog;
import com.neep.neepmeat.api.processing.PowerUtils;
import com.neep.neepmeat.init.NMFluids;
import com.neep.neepmeat.machine.live_machine.LivingMachineComponents;
import com.neep.neepmeat.machine.live_machine.Processes;
import com.neep.neepmeat.machine.live_machine.block.entity.MotorPortBlockEntity;
import com.neep.neepmeat.machine.live_machine.component.PoweredComponent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class LivingMachineBlockEntity extends SyncableBlockEntity implements ComponentHolder
{
    // This looks incredibly cursed
    public static Codec<EnumMap<StructureProperty, AtomicDouble>> PROPERTIES_CODEC = RecordCodecBuilder
            .create(instance ->
                    instance.group(
                            Codec.list(StructureProperty.CODEC).fieldOf("keys").forGetter(o -> new ArrayList<>(o.keySet())),
                            Codec.list(Codec.DOUBLE).fieldOf("values").forGetter(o -> o.values().stream().map(AtomicDouble::get).toList())
                    ).apply(instance, (keys, values) ->
                    {
                        EnumMap<StructureProperty, AtomicDouble> map = new EnumMap<>(StructureProperty.class);
                        for (int i = 0; i < keys.size(); ++i)
                        {
                            map.put(keys.get(i), new AtomicDouble(values.get(i)));
                        }
                        return map;
                    }));

    private final Random random = Random.create();

    protected final List<LivingMachineStructure> structures = new ArrayList<>();
    private final Set<LivingMachineComponent>[] componentMap = (Set<LivingMachineComponent>[]) Array.newInstance(Set.class, ComponentType.Simple.NEXT_ID);
    private final BitSet currentComponents = new BitSet(); // Active components marked in one-hot codes
    private EnumMap<StructureProperty, AtomicDouble> properties = new EnumMap<>(StructureProperty.class);

    protected DegradationManager degradationManager = new DegradationManager(this::degradationRate, Random.create());
    private final float rateMultiplier = 1;

    private final DataLog dataLog;

    protected long age = 0;
    protected int updateInterval = 80;
    protected int maxSize = 100;

    protected float power;
    protected float repairAmount;

    protected boolean updateProcess = true;
    @Nullable private Process process;

    // Convenient combined item output
    private Storage<ItemVariant> combinedItemOutput = Storage.empty();
    private Storage<FluidVariant> combinedFluidInput = Storage.empty();

    // Public, non-persistent round-robin counter
    public int inputSequence;

    // Use only on client. Use structures.size() elsewhere.
    private int numStructures;
    private int numComponents;
    private final List<Identifier> components = new ArrayList<>();
    private Text processName = Process.NO_PROCESS;

    public LivingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        dataLog = new DataLog(500, 40);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        this.age = nbt.getLong("age");
        this.properties = PROPERTIES_CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("properties")).result().orElseGet(() -> new EnumMap<>(StructureProperty.class));
        this.power = nbt.getFloat("power");
        degradationManager.readNbt(nbt);

        // For client only
        this.numStructures = nbt.getInt("num_structures");
        this.numComponents = nbt.getInt("num_components");
        this.processName = Text.of(nbt.getString("process_name"));
        this.repairAmount = nbt.getFloat("repair");

        components.clear();
        NbtList list = nbt.getList("components", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); ++i)
        {
             components.add(Identifier.tryParse(list.getString(i)));
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        nbt.putLong("age", age);
        PROPERTIES_CODEC.encodeStart(NbtOps.INSTANCE, properties).get().ifLeft(r -> nbt.put("properties", r));
        nbt.putFloat("power", power);
        degradationManager.writeNbt(nbt);

        // For rendering on client only
        nbt.putInt("num_structures", structures.size());
        nbt.putInt("num_components", getNumComponents());
        nbt.putString("process_name", getProcess().getString());
        nbt.putFloat("repair", repairAmount);

        NbtList componentList = new NbtList();

        int i = currentComponents.nextSetBit(0);
        while (i != -1 && i < currentComponents.length())
        {
            i = currentComponents.nextSetBit(i);

            ComponentType<?> type = ComponentType.ID_TO_TYPE.get(i);
            Identifier id = ComponentType.REGISTRY.getId(type);
            if (id != null)
                componentList.add(NbtString.of(id.toString()));

            i++;
        }

        nbt.put("components", componentList);
    }

    protected void tickDegradation()
    {
        double repairPerDroplet = 1 / (2 * 81000f); // Decrease in degradation from one droplet
        long maxConsume = 8;
        long consumePerTick = Math.min(MathHelper.ceil(degradationManager.getDegradation() / repairPerDroplet), maxConsume);

//        long consumePerTick = 2; // 2d per tick will consume approximately one bucket per half hour
        AtomicLong satisfied = new AtomicLong();
        withComponents(LivingMachineComponents.INTEGRATION_PORT).ifPresent(w ->
        {
            try (Transaction transaction = Transaction.openOuter())
            {
                for (var port : w.t1())
                {
                    if (satisfied.get() < consumePerTick)
                        satisfied.addAndGet(port.getFluidStorage(null).extract(FluidVariant.of(NMFluids.STILL_WORK_FLUID),
                                consumePerTick - satisfied.get(), transaction));
                    else
                        break;
                }
                transaction.commit();
            }
        });

        repairAmount = 0;

        if (satisfied.get() >= consumePerTick)
        {
            repairAmount += rateMultiplier * repairPerDroplet * satisfied.get();
        }

        float selfRepair = getSelfRepair();
        if (selfRepair > 0)
        {
            repairAmount += rateMultiplier * selfRepair;
        }

//        var motors1 = getComponent(LivingMachineComponents.MOTOR_PORT);

//        if (world.getTime() % 20 == 0 && !motors1.isEmpty())
//        {
//            NeepMeat.LOGGER.info("Age: {}, Efficiency: {}, Rate: {}", 100 * degradationManager.getDegradation(), 100 * getEfficiency(), 100 * 20 * degradationRate(degradationManager.getDegradation()));
//        }

        degradationManager.tick();
    }

    public void serverTick()
    {
        if (age % updateInterval == 0)
        {
            updateStructure();
            sync();
        }

        if (updateProcess)
        {
            updateSpecialStorage();

            this.process = Processes.getInstance().getFirstMatch(currentComponents);
            updateProcess = false;
        }

        age++;
        tickDegradation();

        Collection<MotorPortBlockEntity> motors = getComponent(LivingMachineComponents.MOTOR_PORT);
        float nextPower = 0;
        if (!motors.isEmpty())
        {
            for (var motor : motors)
            {
                nextPower = Math.max(nextPower, motor.getPower());
            }
        }
        else
        {
            nextPower = 0;
        }
        power = nextPower;

        int i = currentComponents.nextSetBit(0);
        while (i != -1 && i < currentComponents.length())
        {
            i = currentComponents.nextSetBit(i);

            for (Iterator<LivingMachineComponent> it = componentMap[i].iterator(); it.hasNext();)
            {
                LivingMachineComponent component = it.next();
                if (component.componentRemoved())
                {
                    it.remove();
                    removeComponent(component);
                    sync();
                    updateProcess = true;
                }
                else
                {
                    if (component instanceof PoweredComponent powered)
                    {
                        powered.setProgressIncrement(getProgressIncrement());
                    }
                }
            }

            ++i;
        }

        if (process != null)
        {
            process.serverTick(this);
        }

        dataLog.log(getWorld().getTime(), this);
    }

    protected void updateStructure()
    {
        search(getPos());
        processStructure();
        updateSpecialStorage();
        updateProcess = true;
    }

    protected void processStructure()
    {
        // Is this bad? Should I be using AtomicDouble and AtomicInteger in single-threaded code? Leave your answer in the comments below.

        EnumMap<StructureProperty, AtomicInteger> present = new EnumMap<>(StructureProperty.class);
        for (var structure : structures)
        {
            structure.getProperties().forEach((property, entry) ->
            {
                if (entry.function().average())
                    present.computeIfAbsent(property, p -> new AtomicInteger(0)).incrementAndGet();
            });
        }

        properties.clear();
        for (var structure : structures)
        {
            structure.getProperties().forEach((property, entry) ->
            {
                var numberPresent = present.get(property);
                entry.apply(properties.computeIfAbsent(property, p -> new AtomicDouble(p.defaultValue())), numberPresent != null ? numberPresent.get() : 1);
            });
        }
    }

    private void updateSpecialStorage()
    {
        List<Storage<ItemVariant>> storages = getComponent(LivingMachineComponents.ITEM_OUTPUT).stream().map(l -> l.getStorage(null)).toList();
        combinedItemOutput = new StorageDelegate(storages);

        combinedFluidInput = new CombinedStorage<>(getComponent(LivingMachineComponents.FLUID_INPUT).stream().map(l -> l.getStorage(null)).toList());
    }

    protected float getProperty(StructureProperty property)
    {
        return properties.computeIfAbsent(property, p -> new AtomicDouble(p.defaultValue())).floatValue();
    }

    public <T extends LivingMachineComponent> Collection<T> getComponent(ComponentType<T> type)
    {
        int idx = type.getBitIdx();
        if (!currentComponents.get(idx))
            return Collections.emptySet();

        return (Collection<T>) componentMap[idx];
    }

    protected void addComponent(LivingMachineComponent component)
    {
        int idx = component.getComponentType().getBitIdx();

        if (componentMap[idx] == null)
            componentMap[idx] = new HashSet<>();

        currentComponents.set(idx);
        componentMap[idx].add(component);
    }

    // Assumes that the component has been removed.
    private void removeComponent(LivingMachineComponent component)
    {
        int idx = component.getComponentType().getBitIdx();
        if (!currentComponents.get(idx))
            return;

        if (componentMap[idx] == null || componentMap[idx].isEmpty())
            currentComponents.clear(idx);
//        componentMap[idx].remove(component); // TODO Reintroduce this without concurrent modification
    }

    protected void clearComponents()
    {
        int i = currentComponents.nextSetBit(0);
        while (i != -1 && i <currentComponents.length())
        {
            i = currentComponents.nextSetBit(i);

            currentComponents.clear(i);
            var component = componentMap[i];
            if (component == null)
            {
                NeepMeat.LOGGER.error("Something has gone horribly wrong");
            }
            else
            {
                componentMap[i].clear();
            }

            ++i;
        }
    }

    protected void search(BlockPos start)
    {
        structures.clear();
        clearComponents();

        Set<BlockPos> visited = Sets.newHashSet();
        Queue<BlockPos> queue = Queues.newArrayDeque();
        visited.add(pos);

        queue.add(start);

        while (!queue.isEmpty())
        {
            BlockPos current = queue.poll();
            BlockPos.Mutable mutable = current.mutableCopy();
            for (Direction direction : Direction.values())
            {
                mutable.set(current, direction);

                if (!visited.contains(mutable))
                {
                    visited.add(mutable.toImmutable());

                    BlockState nextState = world.getBlockState(mutable);
                    if (nextState.isAir())
                        continue;

                    LivingMachineComponent component;
                    if (structures.size() + currentComponents.stream().sum() >= maxSize)
                        return;

                    if (nextState.getBlock() instanceof LivingMachineBlock)
                    {
                        structures.clear();
                        clearComponents();
                        return;
                    }

                    if (nextState.getBlock() instanceof LivingMachineStructure structure)
                    {
                        structures.add(structure);
                        queue.add(mutable.toImmutable());
                    }
                    else if ((component = LivingMachineComponent.LOOKUP.find(world, mutable, null)) != null)
                    {
                        component.setController(pos);
                        addComponent(component);
                        queue.add(mutable.toImmutable());
                    }
                }
            }
        }
    }

    public float getProgressIncrement()
    {
        return power * getEfficiency();
    }

    public float getEfficiency()
    {
        // Take into account performance degradation and block types
//        return getProperty(StructureProperty.SPEED)
//                * (1 - degradationManager.getDegradation());
        return (float) (1 - Math.pow(degradationManager.getDegradation(), 4));
    }

    public float getSelfRepair()
    {
        return getProperty(StructureProperty.SELF_REPAIR);
    }

    public float getRatedPower()
    {
        return getProperty(StructureProperty.MAX_POWER) / PowerUtils.referencePower();
    }

    public double degradationRate(double degradation)
    {
        double rate = -repairAmount;
        if (power <= getRatedPower() * 0.75)
        {
            return rate;
        }

        if (power > getRatedPower() * 2)
            rate += (rateMultiplier * (0.00001f * power / getRatedPower()));
        else if (power > getRatedPower())
            rate += (rateMultiplier * (0.000007f * power / getRatedPower()));
        else
            rate += (rateMultiplier * (0.000003f));

        return rate;
    }

    public double getPower()
    {
        return power;
    }

    public double getRulHours()
    {
        return degradationManager.estimateRul() / (20 * 3600.0);
    }

    public long getRulSecs()
    {
        long ticks = degradationManager.estimateRul();
        if (ticks == -1)
            return -1;

        return ticks / 20;
    }

    public float getCurrentDegradationRate()
    {
        return (float) degradationRate(degradationManager.getDegradation());
    }

    public void onBlockRemoved()
    {
        // Remove all components and set their power to 0.
        int i = currentComponents.nextSetBit(0);
        while (i != -1 && i <currentComponents.length())
        {
            i = currentComponents.nextSetBit(i);

            currentComponents.clear(i);
            var component = componentMap[i];
            if (component == null)
            {
                NeepMeat.LOGGER.error("Something has gone horribly wrong");
            }
            else
            {
                componentMap[i].forEach(c ->
                {
                    if (c instanceof PoweredComponent poweredComponent)
                        poweredComponent.setProgressIncrement(0);
                });
                componentMap[i].clear();
            }

            ++i;
        }
    }

    public Random getRandom()
    {
        return random;
    }

    public DataLog getDataLog()
    {
        return dataLog;
    }

    public long getAge()
    {
        return age;
    }

    public float getHealth()
    {
        return 1 - degradationManager.getDegradation();
    }

    public int getNumStructures()
    {
        if (!world.isClient())
            return structures.size();

        return numStructures;
    }

    public int getNumComponents()
    {
        if (!world.isClient())
        {
            int count = 0;
            for (var set : componentMap)
            {
                if (set == null)
                    continue;

                count += set.size();
            }
            return count;
        }

        return numComponents;
    }

    public Text getProcess()
    {
        if (!world.isClient())
        {
            return process != null ? process.getName() : Process.NO_PROCESS;
        }

        return processName != null ? processName : Process.NO_PROCESS;
    }

    public List<Identifier> getDisplayComponents()
    {
        return components;
    }

    public Storage<ItemVariant> getCombinedItemOutput()
    {
        return combinedItemOutput;
    }

    public Storage<FluidVariant> getCombinedFluidInput()
    {
        return combinedFluidInput;
    }

    private class StorageDelegate implements Storage<ItemVariant>
    {
        private final Storage<ItemVariant> storage;
//        private final BlockPos outPos;
//        private final Direction outDir;
//        private final boolean empty;

//        private StorageDelegate(List<Storage<ItemVariant>> storages, BlockPos outPos, Direction outDir, boolean empty)
        private StorageDelegate(List<Storage<ItemVariant>> storages)
        {
            this.storage = new CombinedStorage<>(storages);
//            this.outPos = outPos;
//            this.outDir = outDir;
//            this.empty = empty;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            return storage.insert(resource, maxAmount, transaction);
//            if (!empty && inserted < maxAmount)
//            {
//                long difference = maxAmount - inserted;
//
//                ItemPipeUtil.stackToAny((ServerWorld) world, outPos, outDir, resource, difference, transaction);
//            }
//            return maxAmount;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            return storage.extract(resource, maxAmount, transaction);
        }

        @Override
        public @NotNull Iterator<StorageView<ItemVariant>> iterator()
        {
            return storage.iterator();
        }
    }
}
