package com.neep.neepmeat.item;

import com.neep.meatlib.item.MeatlibItem;
import com.neep.meatlib.registry.ItemRegistry;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.MinecartItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;

public class BaseMinecartItem extends MinecartItem implements MeatlibItem
{
    protected String registryName;
    private final MinecartFactory factory;

    public BaseMinecartItem(String registryName, Settings settings, MinecartFactory factory)
    {
        super(AbstractMinecartEntity.Type.RIDEABLE, settings);
        this.registryName = registryName;
        this.factory = factory;
        ItemRegistry.queue(this);
        DispenserBlock.registerBehavior(this, vanillaDispenserBehaviour(factory));
    }

    public static DispenserBehavior vanillaDispenserBehaviour(MinecartFactory factory)
    {
        return new ItemDispenserBehavior()
        {
            private final ItemDispenserBehavior defaultBehavior = new ItemDispenserBehavior();

            @Override
            public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack)
            {
                BlockState blockState2;
                double g;
                RailShape railShape;
                Direction direction = pointer.getBlockState().get(DispenserBlock.FACING);
                ServerWorld world = pointer.getWorld();
                double d = pointer.getX() + (double) direction.getOffsetX() * 1.125;
                double e = Math.floor(pointer.getY()) + (double) direction.getOffsetY();
                double f = pointer.getZ() + (double) direction.getOffsetZ() * 1.125;
                BlockPos blockPos = pointer.getPos().offset(direction);
                BlockState blockState = world.getBlockState(blockPos);
                railShape = blockState.getBlock() instanceof AbstractRailBlock railBlock ? blockState.get(railBlock.getShapeProperty()) : RailShape.NORTH_SOUTH;
                if (blockState.isIn(BlockTags.RAILS))
                {
                    g = railShape.isAscending() ? 0.6 : 0.1;
                }
                else if (blockState.isAir() && world.getBlockState(blockPos.down()).isIn(BlockTags.RAILS))
                {
                    RailShape railShape22;
                    blockState2 = world.getBlockState(blockPos.down());
                    railShape22 = blockState2.getBlock() instanceof AbstractRailBlock railBlock
                            ? blockState2.get(railBlock.getShapeProperty())
                            : RailShape.NORTH_SOUTH;
                    g = direction == Direction.DOWN || !railShape22.isAscending() ? -0.9 : -0.4;
                }
                else
                {
                    return this.defaultBehavior.dispense(pointer, stack);
                }

                AbstractMinecartEntity minecart = factory.create(world, d, e + g, f);
                if (stack.hasCustomName())
                {
                    minecart.setCustomName(stack.getName());
                }
                world.spawnEntity(minecart);
                stack.decrement(1);
                return stack;
            }

            @Override
            protected void playSound(BlockPointer pointer)
            {
                pointer.getWorld().syncWorldEvent(WorldEvents.DISPENSER_DISPENSES, pointer.getPos(), 0);
            }
        };
    }

    protected ActionResult vanillaPlacement(ItemUsageContext context)
    {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());

        if (!blockState.isIn(BlockTags.RAILS))
            return ActionResult.FAIL;

        ItemStack itemStack = context.getStack();
        if (!world.isClient)
        {
            RailShape railShape = blockState.getBlock() instanceof AbstractRailBlock ? blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            double d = 0.0;
            if (railShape.isAscending())
            {
                d = 0.5;
            }

            AbstractMinecartEntity minecart = factory.create(world, blockPos.getX() + 0.5, blockPos.getY() + 0.0625, blockPos.getZ() + 0.5);

            if (itemStack.hasCustomName())
                minecart.setCustomName(itemStack.getName());

            world.spawnEntity(minecart);
            world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockPos);
        }
        itemStack.decrement(1);
        return ActionResult.success(world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context)
    {
        return vanillaPlacement(context);
    }

    @Override
    public String getRegistryName()
    {
        return registryName;
    }


    @FunctionalInterface
    public interface MinecartFactory
    {
        AbstractMinecartEntity create(World world, double x, double y, double z);
    }
}
