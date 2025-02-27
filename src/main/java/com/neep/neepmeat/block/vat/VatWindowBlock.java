package com.neep.neepmeat.block.vat;

import com.neep.meatlib.item.ItemSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class VatWindowBlock extends VatCasingBlock implements VatComponent
{
    public VatWindowBlock(String registryName, ItemSettings itemSettings, Settings settings)
    {
        super(registryName, itemSettings, settings.nonOpaque().solidBlock(VatWindowBlock::never));
    }

    public static boolean never(BlockState state, BlockView world, BlockPos pos)
    {
        return false;
    }

    public static boolean never(BlockState blockState, BlockView blockView, BlockPos blockPos, EntityType<?> entityType)
    {
        return false;
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        return VoxelShapes.empty();
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos)
    {
        return 1.0f;
    }

//    @Override
//    public boolean isTranslucent(BlockState state, BlockView world, BlockPos pos)
//    {
//        return true;
//    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
//        return NMBlockEntities.VAT_WINDOW.instantiate(pos, state);
        return null;
    }
}
