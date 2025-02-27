package com.neep.neepmeat.transport.block.item_transport;

import com.neep.meatlib.block.BaseBlock;
import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.meatlib.item.ItemSettings;
import com.neep.neepmeat.plc.robot.PLCActuator;
import com.neep.neepmeat.transport.ItemTransport;
import com.neep.neepmeat.transport.api.item_network.RoutingNetwork;
import com.neep.neepmeat.transport.api.pipe.ItemPipe;
import com.neep.neepmeat.transport.item_network.ItemInPipe;
import com.neep.neepmeat.util.MiscUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class PipeDriverBlock extends BaseBlock implements BlockEntityProvider, ItemPipe
{
    public static final BooleanProperty VALID = BooleanProperty.of("valid");

    public PipeDriverBlock(String registryName, ItemSettings itemSettings, Settings settings)
    {
        super(registryName, itemSettings, settings);
        this.setDefaultState(getDefaultState().with(VALID, false));
    }

    @Override
    public long insert(World world, BlockPos pos, BlockState state, Direction direction, ItemInPipe item, TransactionContext transaction)
    {
        return 0;
    }

    @Override
    public boolean canItemEnter(ResourceAmount<ItemVariant> item, World world, BlockPos pos, BlockState state, Direction inFace)
    {
        return false;
    }

    @Override
    public boolean canItemLeave(ResourceAmount<ItemVariant> item, World world, BlockPos pos, BlockState state, Direction outFace)
    {
        return false;
    }

    @Override
    public EnumSet<Direction> getConnections(BlockState state, Predicate<Direction> forbidden)
    {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify)
    {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) world.getBlockEntity(pos, ItemTransport.PIPE_DRIVER_BE).ifPresent(be -> be.getNetwork(null).invalidate());
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify)
    {
        super.neighborUpdate(state, world, pos, block, fromPos, notify);
        if (!world.isClient()) world.getBlockEntity(pos, ItemTransport.PIPE_DRIVER_BE).ifPresent(be -> be.getNetwork(null).invalidate());
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved)
    {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world instanceof ServerWorld serverWorld)
        {
            PipeDriverBlockEntity.emitUpdate(serverWorld, pos, ItemTransport.BFS_MAX_DEPTH);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        super.appendProperties(builder);
        builder.add(VALID);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return MiscUtil.checkType(type, ItemTransport.PIPE_DRIVER_BE, PipeDriverBlockEntity::serverTick, null, world);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return ItemTransport.PIPE_DRIVER_BE.instantiate(pos, state);
    }

    public static class PipeDriverBlockEntity extends SyncableBlockEntity implements PLCActuator, PLCActuator.Provider
    {
        protected final RoutingNetwork network = new RoutingNetworkImpl(pos, () -> (ServerWorld) this.getWorld());

        public PipeDriverBlockEntity(BlockPos pos, BlockState state)
        {
            this(ItemTransport.PIPE_DRIVER_BE, pos, state);
        }

        public PipeDriverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
        {
            super(type, pos, state);
        }

        public RoutingNetwork getNetwork(Void ctx)
        {
            return network;
        }

        public static void emitUpdate(ServerWorld world, BlockPos origin, int maxDepth)
        {
            Set<Long> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>(20);
            int depth = 0;

            queue.add(origin);
            while (!queue.isEmpty() && depth < maxDepth)
            {
                ++depth;
                BlockPos current = queue.poll();

                BlockPos.Mutable mutable = current.mutableCopy();
                for (Direction direction : Direction.values())
                {
                    mutable.set(current, direction);

                    if (visited.contains(mutable.asLong())) continue;
                    visited.add(mutable.asLong());

                    RoutingNetwork net = RoutingNetwork.LOOKUP.find(world, mutable, null);
                    if (net != null)
                    {
                        net.update();
                        return;
                    }

                    ItemPipe nextPipe = ItemTransport.ITEM_PIPE.find(world, mutable, direction.getOpposite());
                    if (nextPipe != null)
                    {
                        queue.add(mutable.toImmutable());
                    }
                }
            }
        }

        public static void serverTick(World world, BlockPos pos, BlockState state, PipeDriverBlockEntity be)
        {
            if (be.network.needsUpdate())
            {
                be.network.update();
                world.setBlockState(pos, state.with(VALID, be.network.isValid()));
            }
        }

        @Override
        public PLCActuator getPlcActuator()
        {
            return this;
        }

        @Override
        public BlockPos getBasePos()
        {
            return pos;
        }

        @Override
        public void spawnItem(@Nullable ResourceAmount<ItemVariant> stored)
        {
            if (stored == null)
                return;
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5, stored.resource().toStack((int) stored.amount()));
        }

        @Override
        public boolean actuatorRemoved()
        {
            return isRemoved();
        }
    }
}