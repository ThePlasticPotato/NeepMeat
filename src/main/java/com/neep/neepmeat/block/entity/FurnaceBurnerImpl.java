package com.neep.neepmeat.block.entity;

import com.neep.neepmeat.api.Burner;
import com.neep.neepmeat.machine.HeatableFurnace;
import com.neep.neepmeat.mixin.FurnaceAccessor;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FurnaceBurnerImpl implements Burner
{
    protected final FurnaceAccessor furnace;
    private long outputPower;

    public FurnaceBurnerImpl(FurnaceAccessor furnace)
    {
        this.furnace = furnace;
    }

    @Override
    public void tickPowerConsumption()
    {
        Burner.super.tickPowerConsumption();

        // Prevent overunity
        if (((HeatableFurnace) furnace).neepMeat$getHeat() > 0)
        {
            outputPower = 0;
            return;
        }

        if (furnace.getBurnTime() == 0)
        {
            ItemStack itemStack = furnace.getInventory().get(1);
            int time = furnace.callGetFuelTime(itemStack);
            furnace.setFuelTime(time);
            furnace.setBurnTime(time);

            // Consume fuel stack or replace with the recipe remainder
            Item remainder = itemStack.getItem().getRecipeRemainder();
            if (remainder == null) itemStack.decrement(1);
            else
            {
                furnace.getInventory().set(1, new ItemStack(remainder));
            }
            updateBlockstate();
        }
        else
        {
            updateBlockstate();
        }
        furnace.setCookTime(0);
        outputPower = furnace.getBurnTime() > 0 ? 80 : 0;
    }

    @Override
    public double getOutputPower()
    {
        return outputPower;
    }

    protected void updateBlockstate()
    {
        AbstractFurnaceBlockEntity furnaceBE = (AbstractFurnaceBlockEntity) (furnace);
        World world = furnaceBE.getWorld();
        world.setBlockState(furnaceBE.getPos(), furnaceBE.getCachedState().with(AbstractFurnaceBlock.LIT, furnace.getBurnTime() > 0));
    }

    public static Burner get(FurnaceBlockEntity be, Void ctx)
    {
        return new FurnaceBurnerImpl((FurnaceAccessor) be);
    }
}
