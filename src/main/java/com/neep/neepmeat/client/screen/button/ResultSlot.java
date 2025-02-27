package com.neep.neepmeat.client.screen.button;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.screen.slot.Slot;

public class ResultSlot extends Slot
{
    private final PlayerEntity player;
    private int amount; // I have no idea why I have done this, but it seems important.

    public ResultSlot(PlayerEntity player, CraftingInventory input, Inventory inventory, int index, int x, int y)
    {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean canInsert(ItemStack stack)
    {
        return false;
    }

    @Override
    public ItemStack getStack()
    {
        return super.getStack();
    }

    @Override
    public ItemStack takeStack(int amount)
    {
        if (this.hasStack())
        {
            this.amount += Math.min(amount, this.getStack().getCount());
        }
        return super.takeStack(amount);
    }

    @Override
    public void onQuickTransfer(ItemStack newItem, ItemStack original)
    {
        super.onQuickTransfer(newItem, original);
    }

    @Override
    protected void onCrafted(ItemStack stack, int amount)
    {
        this.amount += amount;
        this.onCrafted(stack);
    }

    @Override
    protected void onTake(int amount) {
        this.amount += amount;
    }

    @Override
    protected void onCrafted(ItemStack stack)
    {
        if (this.amount > 0)
        {
            stack.onCraft(this.player.getWorld(), this.player, this.amount);
        }
        if (this.inventory instanceof RecipeUnlocker)
        {
//            ((RecipeUnlocker) this.inventory).unlockLastRecipe(this.player, );
        }
        this.amount = 0;
    }

    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack)
    {
        this.onCrafted(stack);
//        DefaultedList<ItemStack> defaultedList = player.world.getRecipeManager().getRemainingStacks(RecipeType.CRAFTING, this.input, player.world);
//        for (int i = 0; i < defaultedList.size(); ++i)
//        {
//            ItemStack itemStack = this.input.getStack(i);
//            ItemStack itemStack2 = defaultedList.get(i);
//            if (!itemStack.isEmpty())
//            {
//                this.input.removeStack(i, 1);
//                itemStack = this.input.getStack(i);
//            }
//            if (itemStack2.isEmpty()) continue;
//            if (itemStack.isEmpty())
//            {
//                this.input.setStack(i, itemStack2);
//                continue;
//            }
//            if (ItemStack.areItemsEqualIgnoreDamage(itemStack, itemStack2) && ItemStack.areNbtEqual(itemStack, itemStack2))
//            {
//                itemStack2.increment(itemStack.getCount());
//                this.input.setStack(i, itemStack2);
//                continue;
//            }
//            if (this.player.getInventory().insertStack(itemStack2)) continue;
//            this.player.dropItem(itemStack2, false);
//        }
    }
}
