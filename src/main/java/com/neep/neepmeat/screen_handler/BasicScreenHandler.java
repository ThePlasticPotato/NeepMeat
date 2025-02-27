package com.neep.neepmeat.screen_handler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

public abstract class BasicScreenHandler extends ScreenHandler
{
    protected Inventory inventory;
    protected PlayerInventory playerInventory;
    @Nullable protected PropertyDelegate propertyDelegate;

    protected BasicScreenHandler(@Nullable ScreenHandlerType<?> type, PlayerInventory playerInventory, @Nullable Inventory inventory, int syncId, @Nullable PropertyDelegate delegate)
    {
        super(type, syncId);
        this.propertyDelegate = delegate;
        this.inventory = inventory;
        this.playerInventory = playerInventory;

        if (inventory != null) inventory.onOpen(playerInventory.player);

        if (propertyDelegate != null)
            this.addProperties(delegate);
    }

    public int getProperty(int i)
    {
        if (propertyDelegate != null) return propertyDelegate.get(i);
        return 0;
    }

    public void setProperty(int i, int value)
    {
        if (propertyDelegate != null) propertyDelegate.set(i, value);
    }

    public static int playerInvH()
    {
        return 58 + 18 + 2;
    }

//    public static int playerHotbarH()
//    {
//        return 18 + 2;
//    }

    public static int playerSlotsW()
    {
        return 9 * 18 + 2;
    }

    protected void createPlayerSlots(int startX, int startY, PlayerInventory playerInventory)
    {
        createInventory(startX, startY, playerInventory);
        createHotbar(startX, startY + 58, playerInventory);
    }

    protected void createInventory(int startX, int startY, PlayerInventory playerInventory)
    {
        for (int m = 0; m < 3; ++m)
        {
            for (int l = 0; l < 9; ++l)
            {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, startX + l * 18, startY + m * 18));
            }
        }
    }

    protected void createHotbar(int startX, int startY, PlayerInventory playerInventory)
    {
        for (int m = 0; m < 9; ++m)
        {
            this.addSlot(new Slot(playerInventory, m, startX + m * 18, startY));
        }
    }

    protected void createSlotBlock(int startX, int startY, int nx, int ny, Inventory inventory, int startIndex, SlotConstructor constructor)
    {
        int m, l;
        for (m = 0; m < ny; ++m)
        {
            for (l = 0; l < nx; ++l)
            {
                this.addSlot(constructor.construct(inventory, startIndex + l + m * nx, startX + l * 18, startY + m * 18));
            }
        }
    }

    // For some reason, not implementing this causes the game to freeze when shift-clicking.
    @Override
    public ItemStack quickMove(PlayerEntity player, int index)
    {
        ItemStack originalCopy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack())
        {
            ItemStack mutableStack = slot.getStack();
            originalCopy = mutableStack.copy();
            if (index < this.inventory.size())
            {
                if (!this.insertItem(mutableStack, this.inventory.size(), this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(mutableStack, originalCopy);
                slot.takeStack(64); // This 'fixes' a strange thing in WorkstationScreenHandler.
                slot.markDirty();
            }
            else if (!this.insertItem(mutableStack, 0, this.inventory.size(), false))
            {
                return ItemStack.EMPTY;
            }

            if (mutableStack.isEmpty())
            {
                slot.setStack(ItemStack.EMPTY);
            }
            else
            {
                slot.markDirty();
            }
        }
        return originalCopy;
    }

    @FunctionalInterface
    public interface SlotConstructor
    {
        Slot construct(Inventory inventory, int index, int x, int y);
    }

    @Override
    public boolean canUse(PlayerEntity player)
    {
        return true;
//        return this.inventory.canPlayerUse(player);
    }
}