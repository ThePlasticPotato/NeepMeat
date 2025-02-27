package com.neep.neepmeat.machine.advanced_integrator;

import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.meatlib.util.ClientComponents;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.api.DataVariant;
import com.neep.neepmeat.api.data.DataUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class AdvancedIntegratorBlockEntity extends SyncableBlockEntity
{
    private final DataStorage storage = new DataStorage(this::sync);
    private final ClientComponents.Holder<AdvancedIntegratorBlockEntity> holder = new ClientComponents.BlockEntityHolder<>(this);

    public AdvancedIntegratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void onUse(PlayerEntity player)
    {
        player.sendMessage(Text.translatable("message." + NeepMeat.NAMESPACE + ".integrator.data",
                DataUtil.formatData(storage.getAmount()),
                DataUtil.formatData(storage.getCapacity())), true);
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        storage.writeNbt(nbt);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        storage.readNbt(nbt);
        super.readNbt(nbt);
    }

    public DataStorage getDataStorage()
    {
        return storage;
    }

    public void serverTick()
    {
        // Passive accumulation. Exceeding this limit requires a Pylon.
        if (storage.getAmount() < 8 * DataUtil.GIEB && world.getTime() % 4 == 0)
        {
            try (Transaction transaction = Transaction.openOuter())
            {
                storage.insert(DataVariant.NORMAL, 4, transaction);
                transaction.commit();
            }
        }
    }

    public void clientTick()
    {
        holder.get().clientTick();
    }

    public static class DataStorage extends SingleVariantStorage<DataVariant>
    {
        private final Runnable finalCallback;

        public DataStorage(Runnable finalCallback)
        {
            this.finalCallback = finalCallback;
        }

        protected void setAmount(long amount)
        {
            this.amount = amount;
        }

        @Override
        protected DataVariant getBlankVariant()
        {
            return DataVariant.BLANK;
        }

        @Override
        protected long getCapacity(DataVariant variant)
        {
            return DataUtil.GIEB * 16;
        }

        @Override
        public long insert(DataVariant insertedVariant, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);

            if ((insertedVariant.equals(variant) || variant.isBlank()) && canInsert(insertedVariant)) {
                long insertedAmount = Math.min(maxAmount, getCapacity(insertedVariant) - amount);

                if (insertedAmount > 0) {
                    updateSnapshots(transaction);

                    if (variant.isBlank()) {
                        variant = insertedVariant;
                        amount = insertedAmount;
                    } else {
                        amount += insertedAmount;
                    }

                    return insertedAmount;
                }
            }

            return 0;
        }

        public void readNbt(NbtCompound nbt)
        {
            this.variant = DataVariant.fromNbt(nbt.getCompound("variant"));
            this.amount = nbt.getLong("amount");
        }

        @Override
        protected void onFinalCommit()
        {
            super.onFinalCommit();
            finalCallback.run();
        }
    }
}
