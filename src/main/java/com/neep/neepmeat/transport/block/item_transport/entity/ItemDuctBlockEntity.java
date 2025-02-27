package com.neep.neepmeat.transport.block.item_transport.entity;

import com.neep.neepmeat.api.storage.WritableStackStorage;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.transport.block.item_transport.ItemDuctBlock;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public class ItemDuctBlockEntity extends BlockEntity implements Storage<ItemVariant>
{

    private int transferCooldown = -1;
    private BlockApiCache<Storage<ItemVariant>, Direction> cache;

    protected WritableStackStorage storage;
    private long lastTickTime;

    public ItemDuctBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        this.storage = new WritableStackStorage(null);
    }

    public ItemDuctBlockEntity(BlockPos pos, BlockState state)
    {
        this(NMBlockEntities.ITEM_DUCT_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound tag)
    {
        super.writeNbt(tag);
        storage.writeNbt(tag);
    }

    @Override
    public void readNbt(NbtCompound tag)
    {
        super.readNbt(tag);
        storage.readNbt(tag);
    }

    public void updateApiCache(BlockPos pos, BlockState state)
    {
        if (getWorld() == null || !(getWorld() instanceof ServerWorld))
            return;

        Direction direction = state.get(ItemDuctBlock.FACING);
        cache = BlockApiCache.create(ItemStorage.SIDED, (ServerWorld) getWorld(), pos.offset(direction));
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, ItemDuctBlockEntity blockEntity)
    {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();
        if (!blockEntity.needsCooldown())
        {
            blockEntity.setCooldown(8);
            insertTick(world, pos, state, blockEntity);
        }
    }

    private static void insertTick(World world, BlockPos pos, BlockState state, ItemDuctBlockEntity be)
    {
        if (be.getResource().isBlank())
            return;

        Direction targetDirection = state.get(ItemDuctBlock.FACING).getOpposite();
        if (be.cache == null)
        {
            be.updateApiCache(pos, state);
        }

        Storage<ItemVariant> storage = be.cache.find(targetDirection);

        // Spawn item entities at open ends
        if (storage == null)
        {
            BlockPos offsetPos = pos.offset(targetDirection.getOpposite());
            BlockState offsetState = world.getBlockState(offsetPos);
            if (!offsetState.blocksMovement())
            {
                try (Transaction transaction = Transaction.openOuter())
                {
                    ItemEntity item = new ItemEntity(world,
                            offsetPos.getX() + 0.5,
                            offsetPos.getY() + (targetDirection.getAxis().isHorizontal() ? 0.1 : 0.5),
                            offsetPos.getZ() + 0.5,
                            be.getResource().toStack((int) be.getAmount()),
                            0, 0, 0);
                    be.extract(be.getResource(), be.getAmount(), transaction);
                    world.spawnEntity(item);

                    transaction.commit();
                    return;
                }
            }
            return;
        }

        Transaction transaction = Transaction.openOuter();

        if (storage.supportsInsertion() && be.supportsExtraction() && !be.getResource().isBlank())
        {
            StorageUtil.move(be, storage, type -> true, 1, transaction);
        }

        transaction.commit();
    }

    private void setCooldown(int cooldown)
    {
        this.transferCooldown = cooldown;
    }

    private boolean needsCooldown()
    {
        return this.transferCooldown > 0;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
    {
        return storage.insert(resource, maxAmount, transaction);
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
    {
        return storage.extract(resource, maxAmount, transaction);
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator()
    {
        return storage.iterator();
    }

    public ItemVariant getResource()
    {
        return storage.getResource();
    }

    public long getAmount()
    {
        return storage.getAmount();
    }
}
