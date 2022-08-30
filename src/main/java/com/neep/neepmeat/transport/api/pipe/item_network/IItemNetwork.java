package com.neep.neepmeat.transport.api.pipe.item_network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface IItemNetwork
{
    boolean retrieve(BlockPos to, Direction in, ItemVariant variant, long amount, TransactionContext transaction);

    long eject(BlockPos from, Direction out, ItemVariant variant, long amount, TransactionContext transaction);

    long route(BlockPos from, Direction in, BlockPos to, Direction out, ItemVariant variant, int amount, TransactionContext transaction);
}
