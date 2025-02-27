package com.neep.neepmeat.api.big_block;

import com.google.common.collect.Lists;
import com.neep.meatlib.MeatLib;
import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.neepmeat.machine.live_machine.block.entity.LargeCompressorBlockEntity;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BigBlockStructureEntity extends SyncableBlockEntity
{
    @Nullable protected BlockPos controllerPos;
    protected List<Identifier> apis = Lists.newArrayList();
    @Nullable private BlockApiCache<Void, Void> cache;

    public BigBlockStructureEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void setController(@Nullable BlockPos pos)
    {
        this.controllerPos = pos;
    }

    public void enableApi(BlockApiLookup<?, ?> api)
    {
        apis.add(api.getId());
    }

    public VoxelShape translateShape(VoxelShape outlineShape)
    {
        if (controllerPos == null)
            return VoxelShapes.fullCube();

        BlockPos relative = pos.subtract(controllerPos);
        return outlineShape.offset(-relative.getX(), -relative.getY(), -relative.getZ());
    }

    public VoxelShape translateChopShape(VoxelShape outlineShape)
    {
        if (controllerPos == null)
            return VoxelShapes.fullCube();

        return VoxelShapes.combine(translateShape(outlineShape), VoxelShapes.fullCube(), BooleanBiFunction.AND);
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        if (controllerPos != null)
            nbt.put("controller_pos", NbtHelper.fromBlockPos(controllerPos));

        if (!apis.isEmpty())
        {
            NbtList list = new NbtList();
            for (var id : apis)
            {
                list.add(NbtString.of(id.toString()));
            }
            nbt.put("apis", list);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        if (nbt.contains("controller_pos"))
        {
            this.controllerPos = NbtHelper.toBlockPos(nbt.getCompound("controller_pos"));
        }
        else
            this.controllerPos = null;

        apis.clear();
        if (nbt.contains("apis"))
        {
            NbtList list = nbt.getList("apis", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); ++i)
            {
                String string = list.getString(i);
                apis.add(Identifier.tryParse(string));
            }
        }
    }

    @Nullable
    public BlockPos getControllerPos()
    {
        return controllerPos;
    }

    @Nullable
    protected <T extends BlockEntity> T getControllerBE(Class<T> clazz)
    {
        if (controllerPos == null)
        {
            return null;
        }
        else if (cache == null)
        {
            cache = BlockApiCache.create(MeatLib.VOID_LOOKUP, (ServerWorld) getWorld(), controllerPos);
        }

        @Nullable BlockEntity be = cache.getBlockEntity();
        if (clazz.isInstance(be))
            return (T) be;
        else
            return null;
    }
}
