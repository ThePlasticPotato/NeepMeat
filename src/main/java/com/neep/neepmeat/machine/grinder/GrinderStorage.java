package com.neep.neepmeat.machine.grinder;

import com.neep.neepmeat.init.NMrecipeTypes;
import com.neep.neepmeat.recipe.GrindingRecipe;
import com.neep.neepmeat.storage.WritableStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class GrinderStorage extends SimpleInventory
{
    protected GrinderBlockEntity parent;
    protected WritableStackStorage inputStorage;
    protected WritableStackStorage outputStorage;
    protected XpStorage xpStorage;

    public GrinderStorage(GrinderBlockEntity parent)
    {
        this.parent = parent;
        this.inputStorage = new WritableStackStorage(parent)
        {
            @Override
            public boolean canInsert(ItemVariant resource)
            {
                World world = parent.getWorld();
//                if (world == null)
                    return true;

//                List<?> list = world.getRecipeManager().listAllOfType(NMrecipeTypes.GRINDING);
//                return list.stream().anyMatch(r -> r instanceof GrindingRecipe recipe && recipe.getItemInput().resource().equals(resource));
            }
        };

        this.outputStorage = new WritableStackStorage(parent, 32)
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

    public void writeNbt(NbtCompound nbt)
    {
        inputStorage.writeNbt(nbt);

        NbtCompound xpNbt = new NbtCompound();
        xpStorage.writeNbt(xpNbt);
        nbt.put("xp", xpNbt);
    }

    public void readNbt(NbtCompound nbt)
    {
        inputStorage.readNbt(nbt);

        NbtCompound xpNbt = nbt.getCompound("xp");
        xpStorage.readNbt(xpNbt);
    }

    public WritableStackStorage getOutputStorage()
    {
        return outputStorage;
    }

    public void dropItems(World world, BlockPos pos)
    {
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inputStorage.getAsStack());
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outputStorage.getAsStack());
    }

    public static class XpStorage extends SnapshotParticipant<Float>
    {
        private float xp;

        public float insert(float maxAmount, TransactionContext transaction)
        {
            if (maxAmount > 0)
            {
                updateSnapshots(transaction);
                xp += maxAmount;
                return maxAmount;
            }
            return 0;
        }

        public float extract(float maxAmount, TransactionContext transaction)
        {
            if (maxAmount > 0)
            {
                updateSnapshots(transaction);
                float extracted = Math.min(xp, maxAmount);
                xp -= extracted;
                return extracted;
            }
            return 0;
        }

        public float getAmount()
        {
            return xp;
        }

        public void writeNbt(NbtCompound nbt)
        {
            nbt.putFloat("amount", xp);
        }

        public void readNbt(NbtCompound nbt)
        {
            this.xp = nbt.getFloat("amount");
        }

        @Override
        protected Float createSnapshot()
        {
            return xp;
        }

        @Override
        protected void readSnapshot(Float snapshot)
        {
            this.xp = snapshot;
        }
    }
}
