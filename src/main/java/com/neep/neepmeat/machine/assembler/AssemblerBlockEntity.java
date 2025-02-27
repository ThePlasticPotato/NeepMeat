package com.neep.neepmeat.machine.assembler;

import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.entity.FakePlayerEntity;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.screen_handler.AssemblerScreenHandler;
import com.neep.neepmeat.transport.api.pipe.AbstractBloodAcceptor;
import com.neep.neepmeat.transport.api.pipe.BloodAcceptor;
import com.neep.neepmeat.transport.util.ItemPipeUtil;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AssemblerBlockEntity extends SyncableBlockEntity implements NamedScreenHandlerFactory
{
    public static final int PATTERN_SLOTS = 12;
    public static final int MAX_PROGRESS = 10;
    public static final float MAX_INCREMENT = 5f;
    public static final float MIN_INCREMENT = 1f;
    protected float progress;
    protected float increment = 1;
    protected boolean slotSelectMode;

    // Time for rotor to spin in client
    protected int spinTicks = 0;
    public float currentSpeed = 0;
    public float angle = 0;

    private float powerInput = 0;

    BlockApiCache<Storage<ItemVariant>, Direction> cache;

    protected AssemblerStorage storage;

    protected int targetSize;

    protected BloodAcceptor bloodAcceptor = new AbstractBloodAcceptor()
    {
        @Override
        public float updateInflux(float influx)
        {
            powerInput = influx;
            return influx;
        }

        @Override
        public Mode getMode()
        {
            return Mode.SINK;
        }
    };

    protected PropertyDelegate delegate = new PropertyDelegate()
    {
        @Override
        public int get(int index)
        {
            return switch (index)
            {
                case 0 -> storage.outputSlots;
                case 1 -> targetSize;
                case 2 -> slotSelectMode ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value)
        {
            switch (index)
            {
                case 0: storage.outputSlots = value; break;
                case 1: targetSize = value; break;
                case 2: slotSelectMode = value > 0; break;
            }
            markDirty();
        }

        @Override
        public int size()
        {
            return 3;
        }
    };

    public AssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        this.storage = new AssemblerStorage(this);
    }

    public AssemblerBlockEntity(BlockPos pos, BlockState state)
    {
        this(NMBlockEntities.ASSEMBLER, pos, state);
    }

    @Override
    public Text getDisplayName()
    {
        return Text.translatable("container." + NeepMeat.NAMESPACE + ".assembler");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player)
    {
        Inventory targetInv = new SimpleInventory(12);
        if (cache == null) update();
        BlockEntity be = cache.getBlockEntity();
        if (be instanceof Inventory inv)
        {
            targetInv = inv;
            delegate.set(1, inv.size());
        }
        return new AssemblerScreenHandler(syncId, playerInv, storage.getInventory(), targetInv, delegate);
    }

    public void update()
    {
        if (world instanceof ServerWorld serverWorld)
            cache = BlockApiCache.create(ItemStorage.SIDED, serverWorld, pos.down());
    }

    // Move items from target output slots to internal output buffer
    public boolean removeOutputs(Inventory target)
    {
        for (int i = 0; i < target.size() && i < PATTERN_SLOTS; ++i)
        {
            if (storage.isOutput(delegate, i))
            {
                ItemStack stack = target.getStack(i).copy();

                if (target instanceof SidedInventory sided)
                {
                    // May need to account for direction in order to automate water production from sponge furnaces (why?)
                    if (!sided.canExtract(i, stack, null)) return false;
                }

                ItemStack filterStack = storage.getInventory().getStack(i);
                if (!(filterStack.isEmpty() || ItemStack.areItemsEqual(filterStack, stack)))
                    return false;

                try (Transaction transaction = Transaction.openOuter())
                {
                    int ejected = ItemPipeUtil.stackToAny((ServerWorld) world, pos, getCachedState().get(AssemblerBlock.FACING),
                            ItemVariant.of(target.getStack(i)), target.getStack(i).getCount(), transaction);
                    if (ejected > 0)
                    {
                        target.removeStack(i, ejected);
                        target.markDirty();
                        spawnSmoke((ServerWorld) world, pos, getCachedState().get(AssemblerBlock.FACING), stack);
                        transaction.commit();
                        return true;
                    }
                    transaction.abort();
                }
            }
        }
        return false;
    }

    public void tick(ServerWorld world)
    {
        // Sync when spinTicks reaches zero
        int prevSpinTicks = spinTicks;
        spinTicks = Math.max(0, spinTicks - 1);
        if (prevSpinTicks == 1 && spinTicks == 0) sync();

        if (getPUPower() < 0.05) return;

        increment = (float) MathHelper.lerp(getPUPower(), MIN_INCREMENT, MAX_INCREMENT);

        progress = Math.min(MAX_PROGRESS, progress + increment);

        // Generate cache if needed
        BlockPos down = pos.down();
        if (cache == null)
        {
            cache = BlockApiCache.create(ItemStorage.SIDED, world, down);
        }

        if (progress >= MAX_PROGRESS && cache.getBlockEntity() instanceof Inventory target)
        {
            progress = 0;


            Inventory inventory = storage.getInventory();
            boolean moved = false;
            for (int i = 0; i < target.size() && i < PATTERN_SLOTS; ++i)
            {
                ItemStack patternStack = inventory.getStack(i);
                if (!storage.isOutput(delegate, i) && target.getStack(i).isEmpty() && !patternStack.isEmpty())
                {
                    // Honour valid insertion slots
                    if (target instanceof SidedInventory sided && !sided.canInsert(i, patternStack, null))
                    {
                        break;
                    }

                    moved = true;
                    ItemStack transferStack = storage.findIngredient(patternStack);
                    if (!transferStack.isEmpty())
                    {
                        target.setStack(i, transferStack.copy());
                        target.markDirty();
                        syncAnimation();
                        break;
                    }
                }
            }

            if (!moved)
            {
                if (removeOutputs(target))
                {
                    syncAnimation();
                    return;
                }
            }
        }
    }

    private double getPUPower()
    {
        return powerInput;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AssemblerBlockEntity be)
    {
        be.tick((ServerWorld) world);
    }

    public void syncAnimation()
    {
        this.spinTicks = 5;
        sync();
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        storage.writeNbt(nbt);
        nbt.putInt("spinTicks", spinTicks);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        storage.readNbt(nbt);
        this.spinTicks = nbt.getInt("spinTicks");
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
    }

    public AssemblerStorage getStorage()
    {
        return storage;
    }

    public static void spawnSmoke(ServerWorld world, BlockPos pos, Direction facing, ItemStack item)
    {
        world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 10, 0.04, 0, 0.04, 0.01);
        world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, item),
                pos.getX() + facing.getOffsetX() * 0.5 + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + facing.getOffsetZ() * 0.5 + 0.5,
                20, 0.06, 0.06, 0.06, 0.01);
    }

    public static BloodAcceptor getBloodAcceptorFromTop(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction direction)
    {
        if (world.getBlockEntity(pos.down()) instanceof AssemblerBlockEntity be)
        {
            return be.bloodAcceptor;
        }
        return null;
    }
}
