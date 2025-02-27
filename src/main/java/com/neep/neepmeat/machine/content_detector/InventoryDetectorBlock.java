package com.neep.neepmeat.machine.content_detector;

import com.neep.meatlib.block.BaseFacingBlock;
import com.neep.meatlib.item.ItemSettings;
import com.neep.meatlib.storage.MeatlibStorageUtil;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.util.MiscUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class InventoryDetectorBlock extends BaseFacingBlock implements BlockEntityProvider
{
    public static final BooleanProperty POWERED = Properties.POWERED;

    public InventoryDetectorBlock(String itemName, ItemSettings itemSettings, Settings settings)
    {
        super(itemName, itemSettings, settings.nonOpaque().solidBlock(InventoryDetectorBlock::never));
        this.setDefaultState(getDefaultState().with(POWERED, false));
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos)
    {
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
    {
        if (!world.isClient)
        {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null)
            {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    @Nullable
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory) blockEntity : null;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved)
    {
        if (!world.isClient && state.get(POWERED) && world.getBlockTickScheduler().isQueued(pos, this))
        {
            this.updateNeighbors(world, pos, state.with(POWERED, false));
        }

        // Dump contents if destroyed
        if (!newState.isOf(state.getBlock()))
        {
            world.getBlockEntity(pos, NMBlockEntities.INVENTORY_DETECTOR).ifPresent(be ->
                    MeatlibStorageUtil.scatterNoTransaction(world, pos, be.getStorage(null)));
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    protected void updateNeighbors(World world, BlockPos pos, BlockState state)
    {
        Direction direction = state.get(FACING);
        BlockPos newPos = pos.offset(direction.getOpposite());
        world.updateNeighbor(newPos, this, pos);
        world.updateNeighborsExcept(newPos, this, direction);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack)
    {
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context)
    {
        return this.getDefaultState().with(FACING, context.getPlayerLookDirection().getOpposite().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return new InventoryDetectorBlockEntity(pos, state);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state)
    {
        return true;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction)
    {
        return state.getWeakRedstonePower(world, pos, direction);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction)
    {
        if (state.get(POWERED) && state.get(FACING) == direction)
        {
            return 15;
        }
        return 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return MiscUtil.checkType(type, NMBlockEntities.INVENTORY_DETECTOR, InventoryDetectorBlockEntity::serverTick, null, world);
    }

    @Override
    public String getRegistryName()
    {
        return super.getRegistryName();
    }

    public static boolean never(BlockState state, BlockView world, BlockPos pos)
    {
        return false;
    }
}