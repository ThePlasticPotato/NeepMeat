package com.neep.neepmeat.transport.block.item_transport;

import com.neep.neepmeat.transport.ItemTransport;
import com.neep.neepmeat.transport.api.item_network.RoutablePipe;
import com.neep.neepmeat.transport.api.item_network.RoutingNetwork;
import com.neep.neepmeat.transport.api.pipe.ItemPipe;
import com.neep.neepmeat.transport.block.item_transport.entity.ItemPipeBlockEntity;
import com.neep.neepmeat.transport.fluid_network.node.NodePos;
import com.neep.neepmeat.util.BFSGroupFinder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class RoutingNetworkImpl implements RoutingNetwork
{
    protected boolean needsUpdate = true;
    protected final GroupFinder finder = new GroupFinder();

    protected long version;

    protected Supplier<ServerWorld> worldSupplier;
    protected final BlockPos pos;

//    protected final ObjectList<BlockApiCache<RoutablePipe, Direction>> routablePipes = new ObjectArrayList<>(10);
    protected final Long2ObjectMap<BlockApiCache<RoutablePipe, Direction>> routablePipes = new Long2ObjectOpenHashMap<>();
    protected boolean valid;

    public RoutingNetworkImpl(BlockPos pos, Supplier<ServerWorld> worldSupplier)
    {
        this.pos = pos;
        this.worldSupplier = worldSupplier;
    }

    public static <T> ResourceAmount<T> viewToAmount(StorageView<T> view)
    {
        return new ResourceAmount<>(view.getResource(), view.getAmount());
    }

    @Override
    public void invalidate()
    {
        needsUpdate = true;
        ++version;
    }

    @Override
    public boolean needsUpdate()
    {
        return needsUpdate;
    }

    @Override
    public void update()
    {
        needsUpdate = false;

        routablePipes.clear();
        finder.reset();
        finder.queueBlock(pos);
        finder.loop(ItemTransport.BFS_MAX_DEPTH);

        if (finder.controllers > 1)
        {
            valid = false;
            return;
        }

        routablePipes.putAll(finder.getResult());

        finder.getVisited().forEach(p ->
        {
            if (worldSupplier.get().getBlockEntity(BlockPos.fromLong(p)) instanceof ItemPipeBlockEntity be)
            {
                be.getCache().setNetwork(worldSupplier.get(), RoutingNetworkImpl.this.pos);
            }
        });

        valid = true;
    }

    @Override
    public long getVersion()
    {
        return version;
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    public List<ResourceAmount<ItemVariant>> getAllAvailable(TransactionContext transaction)
    {
        return routablePipes.values().stream()
                .map(c -> c.find(null))
                .filter(Objects::nonNull)
                .flatMap(p -> p.getAvailable(transaction))
                .filter(v -> !v.isResourceBlank())
                .map(RoutingNetworkImpl::viewToAmount)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ResourceAmount::resource, Function.identity(),
                                RoutingNetworkImpl::combine),
                        m -> new ArrayList<>(m.values())));
    }

    protected static <T> ResourceAmount<T> combine(ResourceAmount<T> am1, ResourceAmount<T> am2)
    {
        return new ResourceAmount<>(am1.resource(), am1.amount() + am2.amount());
    }

    @Override
    public boolean route(Predicate<ItemVariant> predicate, long amount, BlockPos inPos, Direction inDir, BlockPos outPos, Direction outDir, RequestType type, TransactionContext transaction)
    {
//        StoragePreconditions.notBlankNotNegative(stack.resource(), stack.amount());

        try (Transaction inner = transaction.openNested())
        {
            var inPipeCache = routablePipes.get(inPos.asLong());
            if (inPipeCache != null)
            {
                RoutablePipe inPipe = inPipeCache.find(null);
                if (inPipe != null)
                {
                    long routed = inPipe.request(predicate, amount, new NodePos(outPos, outDir), inner);
                    if (type.satisfied(amount, routed))
                    {
                        worldSupplier.get().spawnParticles(ParticleTypes.SMOKE, this.pos.getX() + 0.5, this.pos.getY() + 1, this.pos.getZ() + 0.5, 20, 0.1, 0, 0.1, 0.01);
                        worldSupplier.get().playSound(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), SoundEvents.ENTITY_PIGLIN_CELEBRATE, SoundCategory.BLOCKS, 1, 1);
                        inner.commit();
                        return true;
                    }
                }
            }
            inner.abort();
        }
        return false;
    }

    @Override
    public boolean request(Predicate<ItemVariant> predicate, long amount, BlockPos pos, Direction outDir, RequestType type, TransactionContext transaction)
    {
        try (Transaction inner = transaction.openNested())
        {
            AtomicLong amountRemaining = new AtomicLong(amount);
            boolean satisfied = routablePipes.values().stream().anyMatch(e ->
            {
                long retrieved = e.find(null).request(predicate, amountRemaining.get(), new NodePos(pos, outDir), inner);
                amountRemaining.addAndGet(-retrieved);
                return amountRemaining.get() <= 0;
            });

            if (type.satisfied(amount, amount - amountRemaining.get()))
            {
                worldSupplier.get().spawnParticles(ParticleTypes.SMOKE, this.pos.getX() + 0.5, this.pos.getY() + 1, this.pos.getZ() + 0.5, 20, 0.1, 0, 0.1, 0.01);
                worldSupplier.get().playSound(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), SoundEvents.ENTITY_PIGLIN_CELEBRATE, SoundCategory.BLOCKS, 1, 1);
                inner.commit();
                return satisfied;
            }
            else inner.abort();
        }

        return false;
    }

    protected class GroupFinder extends BFSGroupFinder<BlockApiCache<RoutablePipe, Direction>>
    {
        protected int controllers = 0;

        @Override
        public void reset()
        {
            super.reset();
            controllers = 0;
        }

        public Set<Long> getVisited()
        {
            return visited;
        }

        @Override
        protected State processPos(BlockPos pos)
        {
            ItemPipe fromPipe = ItemTransport.ITEM_PIPE.find(worldSupplier.get(), pos, null);

            // Fail if there is a second controller in the network.
            if (checkController(worldSupplier.get(), pos)) return State.FAIL;

            BlockPos.Mutable mutable = pos.mutableCopy();
            for (Direction direction : fromPipe.getConnections(worldSupplier.get().getBlockState(pos), d -> true))
            {
                mutable.set(pos, direction);
                RoutablePipe routablePipe = RoutablePipe.LOOKUP.find(worldSupplier.get(), mutable, null);
                if (routablePipe != null)
                {
                    addResult(mutable, BlockApiCache.create(RoutablePipe.LOOKUP, worldSupplier.get(), mutable));
                }

                ItemPipe toPipe = ItemTransport.ITEM_PIPE.find(worldSupplier.get(), mutable, null);
                if (toPipe != null)
                {
                    queueBlock(mutable);
                }
            }

            return State.CONTINUE;
        }

        protected boolean checkController(ServerWorld world, BlockPos current)
        {
            BlockApiCache<RoutingNetwork, Void> cache = BlockApiCache.create(RoutingNetwork.LOOKUP, (ServerWorld) world, current);
            if (cache.find(null) != null)
            {
                ++controllers;
            }
            return false;
        }
    }
}
