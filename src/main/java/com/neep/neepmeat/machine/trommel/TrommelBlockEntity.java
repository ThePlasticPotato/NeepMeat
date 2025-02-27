package com.neep.neepmeat.machine.trommel;

import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.neepmeat.api.multiblock.ControllerBlockEntity;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.machine.small_trommel.SmallTrommelStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TrommelBlockEntity extends SyncableBlockEntity implements ControllerBlockEntity
{
//    protected TrommelStorage storage;
    protected List<BlockPos> structures = new ArrayList<>();

    public TrommelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
//        storage = new TrommelStorage(this);
    }

    public TrommelBlockEntity(BlockPos pos, BlockState state)
    {
        this(NMBlockEntities.TROMMEL, pos, state);
    }

    public SmallTrommelStorage getStorage()
    {
//        return storage;
        return null;
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
//        storage.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
//        storage.readNbt(nbt);
    }

    public void addStructure(TrommelStructureBlockEntity be)
    {
        structures.add(be.getPos());
    }

    @Override
    public void componentBroken(ServerWorld world)
    {
        world.setBlockState(getPos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    public Storage<FluidVariant> getFluidInput()
    {
        return null;
    }

    public Storage<FluidVariant> getFluidOutput()
    {
        return null;
    }

    public Storage<ItemVariant> getItemOutput()
    {
        return null;
    }
}
