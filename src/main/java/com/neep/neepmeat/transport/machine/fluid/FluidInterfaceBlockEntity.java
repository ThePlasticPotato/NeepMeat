package com.neep.neepmeat.transport.machine.fluid;

import com.neep.neepmeat.api.storage.LazyBlockApiCache;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.transport.block.fluid_transport.FluidInterfaceBlock;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collections;
import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public class FluidInterfaceBlockEntity extends BlockEntity implements Storage<FluidVariant>
{
    protected final LazyBlockApiCache<Storage<FluidVariant>, Direction> cache;

    public FluidInterfaceBlockEntity(BlockEntityType type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        Direction facing = getCachedState().get(FluidInterfaceBlock.FACING);
        BlockPos offset = pos.offset(facing);
        this.cache = LazyBlockApiCache.of(FluidStorage.SIDED, offset, this::getWorld, facing::getOpposite);
    }

    public FluidInterfaceBlockEntity(BlockPos pos, BlockState state)
    {
        this(NMBlockEntities.FLUID_INTERFACE, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound tag)
    {
        super.writeNbt(tag);
    }

    @Override
    public void readNbt(NbtCompound tag)
    {
        super.readNbt(tag);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        Storage<FluidVariant> storage = cache.find();
        if (storage != null)
        {
            return storage.insert(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        Storage<FluidVariant> storage = cache.find();
        if (storage != null)
        {
            return storage.extract(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator()
    {
        Storage<FluidVariant> storage = cache.find();
        if (storage != null)
        {
            return storage.iterator();
        }
        return Collections.emptyIterator();
    }
}
