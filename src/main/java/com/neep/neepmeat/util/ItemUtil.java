package com.neep.neepmeat.util;

import com.neep.neepmeat.item.FluidComponentItem;
import com.neep.neepmeat.transport.api.pipe.FluidPipe;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SuppressWarnings("UnstableApiUsage")
public class ItemUtil
{
    public static boolean containsResource(List<StorageView<ItemVariant>> list, ItemVariant stack)
    {
        for (StorageView<ItemVariant> patternStack : list)
        {
            if (patternStack.getResource().equals(stack))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<StorageView<ItemVariant>> list, StorageView<ItemVariant> observedView, FilterUtils.Filter filter)
    {
        for (StorageView<ItemVariant> patternStack : list)
        {
            if (patternStack.getResource().equals(observedView.getResource())
                    && filter.test(observedView.getAmount(), patternStack.getAmount()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean notBlank(StorageView<ItemVariant> view)
    {
        return !view.isResourceBlank();
    }

    public static boolean singleVariantInteract(PlayerEntity player, Hand hand, SingleVariantStorage<ItemVariant> storage)
    {
        ItemStack stack = player.getStackInHand(hand);
        try (Transaction transaction = Transaction.openOuter())
        {
            if ((storage.isResourceBlank() || storage.getResource().matches(stack)) && !stack.isEmpty())
            {
                long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                stack.decrement((int) inserted);
                transaction.commit();
                return true;
            }
            else if ((stack.isEmpty() || !storage.getResource().matches(stack)) && !storage.isResourceBlank())
            {
                ItemStack giveStack = storage.getResource().toStack((int) storage.getAmount());
                player.giveItemStack(giveStack);
                storage.extract(storage.getResource(), storage.getAmount(), transaction);
                transaction.commit();
                return true;
            }
            else transaction.abort();
        }
        return false;
    }

    public static ItemStack viewToStack(StorageView<ItemVariant> view)
    {
        return view.getResource().toStack((int) view.getAmount());
    }

    public static <T extends TransferVariant<?>> Optional<Long> totalAmount(Storage<T> storage, T resource, Transaction transaction)
    {
        return StreamSupport.stream(storage.spliterator(), false)
                .filter(view -> view.getResource().equals(resource))
                .map(StorageView::getAmount)
                .reduce(Long::sum);
    }

    public static boolean playerHoldingPipe(PlayerEntity player, Hand hand)
    {
        return player.getStackInHand(hand).getItem() instanceof BlockItem blockItem && (blockItem.getBlock() instanceof FluidPipe
                || blockItem instanceof FluidComponentItem);
    }

    public static boolean checkFluidComponent(Item item)
    {
        return item instanceof BlockItem bi && bi instanceof FluidComponentItem;
    }

    public static NbtCompound toNbt(Inventory inventory)
    {
        NbtCompound nbt = new NbtCompound();
        writeInventory(nbt, inventory);
        return nbt;
    }

    public static NbtCompound writeInventory(NbtCompound nbt, Inventory inventory)
    {
        NbtList nbtList = new NbtList();
        for (int i = 0; i < inventory.size(); ++i)
        {
            ItemStack itemStack = inventory.getStack(i);

            if (itemStack.isEmpty()) continue;

            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte) i);
            itemStack.writeNbt(nbtCompound);
            nbtList.add(nbtCompound);
        }
//        if (!nbtList.isEmpty() || setIfEmpty)
        nbt.put("Items", nbtList);
        return nbt;
    }

    public static void readInventory(NbtCompound nbt, Inventory inventory)
    {
        NbtList nbtList = nbt.getList("Items", 10);
        for (int i = 0; i < nbtList.size(); ++i)
        {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 0xFF;
            if (j < 0 || j >= inventory.size()) continue;
            inventory.setStack(j, ItemStack.fromNbt(nbtCompound));
        }
    }
}
