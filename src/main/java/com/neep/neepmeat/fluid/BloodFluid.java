package com.neep.neepmeat.fluid;

import com.neep.neepmeat.init.BlockInitialiser;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.*;

public class BloodFluid extends RealisticFluid
{
    @Override
    public Fluid getStill()
    {
        return BlockInitialiser.STILL_BLOOD;
    }

    @Override
    public Fluid getFlowing()
    {
        return BlockInitialiser.FLOWING_BLOOD;
    }

    @Override
    public Item getBucketItem()
    {
        return BlockInitialiser.BLOOD_BUCKET;
    }

    @Override
    protected BlockState toBlockState(FluidState fluidState)
    {
        return BlockInitialiser.TEST.getDefaultState().with(Properties.LEVEL_15, getBlockStateLevel(fluidState));
    }

    @Override
    public void onScheduledTick(World world, BlockPos pos, FluidState state) {
        if (!state.isStill()) {
//            FluidState fluidState = this.getUpdatedState(world, pos, world.getBlockState(pos));
//            int i = this.getNextTickDelay(world, pos, state, fluidState);
//            if (fluidState.isEmpty()) {
//                state = fluidState;
//                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
//            } else if (!fluidState.equals(state)) {
//                state = fluidState;
//                BlockState blockState = state.getBlockState();
//                world.setBlockState(pos, blockState, Block.NOTIFY_LISTENERS);
//                world.getFluidTickScheduler().schedule(pos, state.getFluid(), i);
//                world.updateNeighborsAlways(pos, blockState.getBlock());
//            }
        }
        this.tryFlow(world, pos, state);
    }

    protected void tryFlow(WorldAccess world, BlockPos fluidPos, FluidState state)
    {
        if (state.isEmpty())
        {
            return;
        }

        // Try to distribute current level across surrounding blocks.

        List<BlockPos> posList = new ArrayList<>();
        for (Direction direction : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST})
        {
            BlockPos targetPos = fluidPos.offset(direction, 1);
            BlockState targetState = world.getBlockState(targetPos);
            int targetLevel = targetState.getFluidState().getLevel();
            if (canFill(world, targetPos, targetState, this) && (targetLevel < state.getLevel())
                    && (state.getLevel() > 1 || direction == Direction.DOWN))
            {
                posList.add(targetPos);
            }
        }

        // Prioritise neighbours with lower fluid levels.

//        posList.sort((blockPos1, blockPos2) ->
//        {
//            int level1 = world.getBlockState(blockPos1).getFluidState().getLevel();
//            int level2 = world.getBlockState(blockPos2).getFluidState().getLevel();
//            return Integer.compare(level1, level2);
//        });

        // Distribute fluid across list of positions
        int level = state.getLevel();
        for (BlockPos blockPos : posList)
        {
            // Break if there is too little fluid, but also allow flowing down
            if (!(level > 1 || fluidPos.offset(Direction.DOWN).equals(blockPos)))
                break;

            BlockPos pos = blockPos;
            BlockState targetState = world.getBlockState(pos);
            int targetLevel = targetState.getFluidState().getLevel();

//            if (targetState.isAir())
//            {
//                world.setBlockState(pos, this.getFlowing(1, false).getBlockState(), Block.NOTIFY_ALL);
//                --level;
//            }
//            else if (targetState.getFluidState().getFluid().equals(this))
//            {
//                world.setBlockState(pos, this.getFlowing(targetLevel + 1, false).getBlockState(), Block.NOTIFY_ALL);
//                --level;
//            }
            world.setBlockState(pos, this.getFlowing(targetLevel + 1, false).getBlockState(), Block.NOTIFY_ALL);
            --level;

        }
        if (level != state.getLevel())
        {
            BlockState newState = level > 0 ? this.getFlowing(level, false).getBlockState() : Blocks.AIR.getDefaultState();
            world.setBlockState(fluidPos, newState, Block.NOTIFY_ALL);
        }
    }

    private int getNearby(WorldView world, BlockPos pos) {
        int i = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos blockPos = pos.offset(direction);
            FluidState fluidState = world.getFluidState(blockPos);
            if (!this.isMatchingAndStill(fluidState)) continue;
            ++i;
        }
        return i;
    }

    private boolean isMatchingAndStill(FluidState state)
    {
        return state.getFluid().matchesType(this) && state.isStill();
    }

    private boolean method_15736(BlockView world, Fluid fluid, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
        if (!this.receivesFlow(Direction.DOWN, world, pos, state, fromPos, fromState)) {
            return false;
        }
        if (fromState.getFluidState().getFluid().matchesType(this)) {
            return true;
        }
        return this.canFill(world, fromPos, fromState, fluid);
    }

    private boolean receivesFlow(Direction face, BlockView world, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState)
    {
        return true;
    }

    private boolean canFill(BlockView world, BlockPos pos, BlockState state, Fluid fluid)
    {
        Block block = state.getBlock();
        if (block instanceof FluidFillable) {
            return ((FluidFillable)((Object)block)).canFillWithFluid(world, pos, state, fluid);
        }
        if (block instanceof DoorBlock || state.isIn(BlockTags.SIGNS) || state.isOf(Blocks.LADDER) || state.isOf(Blocks.SUGAR_CANE) || state.isOf(Blocks.BUBBLE_COLUMN)) {
            return false;
        }
        Material material = state.getMaterial();
        if (material == Material.PORTAL || material == Material.STRUCTURE_VOID || material == Material.UNDERWATER_PLANT || material == Material.REPLACEABLE_UNDERWATER_PLANT) {
            return false;
        }
        return !material.blocksMovement();
    }

    private void moveThing(WorldAccess world, BlockPos pos, FluidState fluidState, BlockState blockState)
    {
        int i = fluidState.getLevel() - this.getLevelDecreasePerBlock(world);
        if (fluidState.get(FALLING).booleanValue())
        {
            i = 7;
        }
        if (i <= 0)
        {
            return;
        }
        Map<Direction, FluidState> map = this.getSpread(world, pos, blockState);
        for (Map.Entry<Direction, FluidState> entry : map.entrySet())
        {
            BlockState blockState2;
            Direction direction = entry.getKey();
            FluidState fluidState2 = entry.getValue();
            BlockPos blockPos = pos.offset(direction);
            if (!this.canFlow(world, pos, blockState, direction, blockPos, blockState2 = world.getBlockState(blockPos), world.getFluidState(blockPos), fluidState2.getFluid())) continue;
            this.flow(world, blockPos, blockState2, direction, fluidState2);
        }
    }

    public static class Flowing extends BloodFluid
    {
        public Flowing()
        {
            setDefaultState(getStateManager().getDefaultState().with(LEVEL, 8));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder)
        {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getLevel(FluidState fluidState)
        {
            return fluidState.get(LEVEL);
        }

        @Override
        public boolean isStill(FluidState fluidState)
        {
            return true;
        }
    }

    public static class Still extends BloodFluid
    {
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder)
        {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getLevel(FluidState fluidState)
        {
            return fluidState.get(LEVEL);
        }

        @Override
        public boolean isStill(FluidState fluidState)
        {
            return false;
        }
    }
}
