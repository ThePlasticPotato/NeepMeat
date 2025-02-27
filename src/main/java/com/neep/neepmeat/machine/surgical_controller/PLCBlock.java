package com.neep.neepmeat.machine.surgical_controller;

import com.neep.meatlib.block.BaseHorFacingBlock;
import com.neep.meatlib.item.ItemSettings;
import com.neep.neepmeat.plc.PLCBlocks;
import com.neep.neepmeat.plc.block.entity.PLCBlockEntity;
import com.neep.neepmeat.transport.api.pipe.DataCable;
import com.neep.neepmeat.util.MiscUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PLCBlock extends BaseHorFacingBlock implements BlockEntityProvider, DataCable
{
    public PLCBlock(String itemName, ItemSettings itemSettings, Settings settings)
    {
        super(itemName, itemSettings, settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
    {
//         TODO: Client separation
        if (world.getBlockEntity(pos) instanceof PLCBlockEntity be)
        {
            be.enter(player);
        }

//        if (world.getBlockEntity(pos) instanceof PLCBlockEntity be)
//        {
//            be.setCounter(0);
//        }

//        world.getBlockEntity(pos, NMBlockEntities.PLC).ifPresent(be ->
//        {
//            be.showBlocks(player);
//        });

        return ActionResult.SUCCESS;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify)
    {
        super.neighborUpdate(state, world, pos, block, fromPos, notify);
        if (!world.isClient() && world.getBlockEntity(pos) instanceof TableControllerBlockEntity be)
        {
            be.update(world.isReceivingRedstonePower(pos));
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return MiscUtil.checkType(type, PLCBlocks.PLC_ENTITY, (world1, pos, state1, blockEntity) -> blockEntity.tick(), (world1, pos, state1, blockEntity) -> blockEntity.clientTick(), world);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return PLCBlocks.PLC_ENTITY.instantiate(pos, state);
    }
}
