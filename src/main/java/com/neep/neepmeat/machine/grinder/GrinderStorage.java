package com.neep.neepmeat.machine.grinder;

import com.neep.neepmeat.api.storage.WritableStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@SuppressWarnings("UnstableApiUsage")
public class GrinderStorage extends SimpleInventory implements CrusherRecipeContext
{
    protected GrinderBlockEntity parent;
    protected WritableStackStorage inputStorage;
    protected WritableStackStorage outputStorage;
    protected WritableStackStorage extraStorage;
    protected XpStorage xpStorage;

    public GrinderStorage(GrinderBlockEntity parent)
    {
        this.parent = parent;
        this.inputStorage = new WritableStackStorage(parent::sync)
        {
            @Override
            public boolean canInsert(ItemVariant resource)
            {
                return true;
            }
        };

        this.outputStorage = new WritableStackStorage(parent::sync, 32)
        {
            @Override
            public boolean supportsInsertion()
            {
                return false;
            }
        };

        this.extraStorage = new WritableStackStorage(parent::sync, 32)
        {
            @Override
            public boolean supportsInsertion()
            {
                return false;
            }
        };

        this.xpStorage = new XpStorage();
    }

    public Storage<ItemVariant> getItemStorage(Direction direction)
    {
        if (direction == Direction.UP || direction == null)
        {
            return inputStorage;
        }
        else if (direction == parent.getCachedState().get(GrinderBlock.FACING) || direction == Direction.DOWN)
        {
            return outputStorage;
        }
        return null;
    }

    public WritableStackStorage getInputStorage()
    {
        return inputStorage;
    }

    public XpStorage getXpStorage()
    {
        return xpStorage;
    }

    @Override
    public float getChanceMod()
    {
        return 0;
    }

    public void writeNbt(NbtCompound nbt)
    {
        NbtCompound inputNbt = new NbtCompound();
        inputStorage.writeNbt(inputNbt);
        nbt.put("input", inputNbt);

        NbtCompound outputNbt = new NbtCompound();
        outputStorage.writeNbt(outputNbt);
        nbt.put("output", outputNbt);

        NbtCompound extraNbt = new NbtCompound();
        extraStorage.writeNbt(extraNbt);
        nbt.put("extra", extraNbt);

        NbtCompound xpNbt = new NbtCompound();
        xpStorage.writeNbt(xpNbt);
        nbt.put("xp", xpNbt);
    }

    public void readNbt(NbtCompound nbt)
    {
        NbtCompound inputNbt = nbt.getCompound("input");
        inputStorage.readNbt(inputNbt);

        NbtCompound outputNbt = nbt.getCompound("output");
        outputStorage.readNbt(outputNbt);

        NbtCompound extraNbt = nbt.getCompound("extra");
        extraStorage.readNbt(extraNbt);

        NbtCompound xpNbt = nbt.getCompound("xp");
        xpStorage.readNbt(xpNbt);
    }

    public Storage<ItemVariant> getOutputStorage()
    {
        return outputStorage;
    }

    public Storage<ItemVariant> getExtraStorage()
    {
        return extraStorage;
    }

    public void dropItems(World world, BlockPos pos)
    {
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inputStorage.getAsStack());
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outputStorage.getAsStack());
    }

}
