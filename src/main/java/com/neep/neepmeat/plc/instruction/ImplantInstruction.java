package com.neep.neepmeat.plc.instruction;

import com.neep.meatlib.recipe.MeatlibRecipes;
import com.neep.neepmeat.api.plc.PLC;
import com.neep.neepmeat.api.plc.recipe.Workpiece;
import com.neep.neepmeat.api.plc.robot.AtomicAction;
import com.neep.neepmeat.api.plc.robot.DelayAction;
import com.neep.neepmeat.api.plc.robot.GroupedRobotAction;
import com.neep.neepmeat.api.plc.robot.RobotAction;
import com.neep.neepmeat.api.storage.LazyBlockApiCache;
import com.neep.neepmeat.init.NMComponents;
import com.neep.neepmeat.init.NMSounds;
import com.neep.neepmeat.plc.Instructions;
import com.neep.neepmeat.plc.component.MutateInPlace;
import com.neep.neepmeat.plc.recipe.EntityMutateRecipe;
import com.neep.neepmeat.plc.recipe.ImplantStep;
import com.neep.neepmeat.plc.recipe.PLCRecipes;
import com.neep.neepmeat.plc.robot.RobotMoveToAction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class ImplantInstruction implements Instruction
{
    private final Supplier<World> world;
    private final LazyBlockApiCache<MutateInPlace<Entity>, Void> toCache;
    private final Argument from;
    private final Argument to;
    private ResourceAmount<ItemVariant> stored;

    private final GroupedRobotAction group;

    public ImplantInstruction(Supplier<World> world, List<Argument> arguments)
    {
        this.world = world;
        this.from = arguments.get(0);
        this.to = arguments.get(1);
//        LazyBlockApiCache<Storage<ItemVariant>, Direction> fromCache = LazyBlockApiCache.itemSided(from, () -> (ServerWorld) world.get());
        this.toCache = LazyBlockApiCache.of(MutateInPlace.ENTITY, to.pos(), world, () -> null);

        group = GroupedRobotAction.of(
                new RobotMoveToAction(from.pos()),
                AtomicAction.of(this::takeFrom),
                new Glue(),
                AtomicAction.of(this::playSound),
                new DelayAction(40),
                AtomicAction.of(this::install)
        );
    }

    private void playSound(PLC plc)
    {
        var robot = plc.getActuator();
        world.get().playSound(null, robot.getX(), robot.getY(), robot.getZ(), NMSounds.IMPLANT_INSTRUCTION_APPLY, SoundCategory.NEUTRAL, 1, 1, 1);
    }

    public ImplantInstruction(Supplier<World> world, NbtCompound compound)
    {
        this(world, List.of(
                Argument.fromNbt(compound.getCompound("from")),
                Argument.fromNbt(compound.getCompound("to"))
        ));
        group.readNbt(compound.getCompound("action"));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt)
    {
        nbt.put("from", from.toNbt());
        nbt.put("to", to.toNbt());
        nbt.put("action", group.writeNbt(new NbtCompound()));
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {

    }

    @Override
    public boolean canStart(PLC plc)
    {
        return true;
    }

    @Override
    public void start(PLC plc)
    {
        plc.addRobotAction(group, this::finish);
    }

    @Override
    public void cancel(PLC plc)
    {
        plc.getActuator().spawnItem(stored);
        group.end(plc);
        stored = null;
    }

    private void takeFrom(PLC plc)
    {
        var takenAmount = Instructions.takeItem(from, world, 1);
        if (takenAmount != null)
        {
            this.stored = takenAmount;
        }
    }

    private void install(PLC plc)
    {
        var mip = toCache.find();
        if (mip != null && stored != null)
        {
            ImplantStep step = new ImplantStep(stored.resource().getItem());

            Entity entity = mip.get();
            Workpiece workpiece;
            if (entity != null && (workpiece = NMComponents.WORKPIECE.getNullable(entity)) != null)
            {
                step.mutate(entity);

                // I really need an elegant way to check multiple recipe types simultaneously
                EntityMutateRecipe recipe = MeatlibRecipes.getInstance().getFirstMatch(PLCRecipes.ENTITY_MANUFACTURE, mip).orElse(null);
                if (recipe == null)
                    recipe = MeatlibRecipes.getInstance().getFirstMatch(PLCRecipes.ENTITY_TO_ITEM, mip).orElse(null);

                if (recipe != null)
                {
                    recipe.ejectOutputs(mip, null);
                    workpiece.clearSteps();
                }

                stored = null;
            }
        }

        if (stored != null)
            plc.getActuator().spawnItem(stored);
    }

    private void finish(PLC plc)
    {
        plc.advanceCounter();
    }

    private BlockPos getEntityPos(PLC plc)
    {
        var mip = toCache.find();
        if (mip != null && mip.get() != null)
        {
            Entity entity = mip.get();
            return new BlockPos(entity.getBlockX(),
                    (int) Math.ceil(entity.getY() + entity.getHeight()),
                    entity.getBlockZ());
        }
        else
        {
            plc.raiseError(new PLC.Error("No entity station found"));
            plc.getActuator().spawnItem(stored);
        }
        return toCache.pos();
    }

    @Override
    public @NotNull InstructionProvider getProvider()
    {
        return Instructions.IMPLANT;
    }

    class Glue implements RobotAction
    {
        @Override
        public boolean finished(PLC plc)
        {
            return plc.getActuator().reachedTarget(plc);
        }

        @Override
        public void start(PLC plc)
        {
            plc.getActuator().setTarget(plc, getEntityPos(plc));
        }

        @Override
        public void tick(PLC plc)
        {
        }

        @Override
        public void end(PLC plc)
        {

        }
    }
}
