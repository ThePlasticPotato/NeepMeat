package com.neep.neepmeat.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.neep.neepmeat.block.base.BaseBlock;
import com.neep.neepmeat.fluid_util.FluidNetwork;
import com.neep.neepmeat.fluid_util.PipeConnectionType;
import com.neep.neepmeat.fluid_util.PipeProperties;
import com.neep.neepmeat.fluid_util.node.FluidNode;
import com.neep.neepmeat.fluid_util.node.NodePos;
import com.neep.neepmeat.util.NMMaths;
import com.neep.neepmeat.util.NMVec2f;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Map;

public class PipeBlock extends BaseBlock implements FluidAcceptor
{
    public static final BooleanProperty ooer = BooleanProperty.of("abc");
    public static final EnumProperty<PipeConnectionType> NORTH_CONNECTION = EnumProperty.of("north", PipeConnectionType.class);
    public static final EnumProperty<PipeConnectionType> EAST_CONNECTION = PipeProperties.EAST_CONNECTION;
    public static final EnumProperty<PipeConnectionType> SOUTH_CONNECTION = PipeProperties.SOUTH_CONNECTION;
    public static final EnumProperty<PipeConnectionType> WEST_CONNECTION = PipeProperties.WEST_CONNECTION;
    public static final EnumProperty<PipeConnectionType> UP_CONNECTION = PipeProperties.UP_CONNECTION;
    public static final EnumProperty<PipeConnectionType> DOWN_CONNECTION = PipeProperties.DOWN_CONNECTION;

    private static final Map<BlockState, VoxelShape> SHAPES = Maps.newHashMap();

    public static final Map<Direction, EnumProperty<PipeConnectionType>> DIR_TO_CONNECTION = (new ImmutableMap.Builder<Direction, EnumProperty<PipeConnectionType>>()
            .put(Direction.NORTH, NORTH_CONNECTION)
            .put(Direction.EAST, EAST_CONNECTION)
            .put(Direction.SOUTH, SOUTH_CONNECTION)
            .put(Direction.WEST, WEST_CONNECTION)
            .put(Direction.DOWN, DOWN_CONNECTION)
            .put(Direction.UP, UP_CONNECTION)
    ).build();

    public static final Map<Direction, VoxelShape> DIR_SHAPES = (new ImmutableMap.Builder<Direction, VoxelShape>()
            .put(Direction.NORTH, Block.createCuboidShape(4, 4, 0, 12, 12, 5))
            .put(Direction.EAST, Block.createCuboidShape(11, 4, 4, 16, 12, 12))
            .put(Direction.SOUTH, Block.createCuboidShape(4, 4, 11, 12, 12, 16))
            .put(Direction.WEST, Block.createCuboidShape(0, 4, 4, 5, 12, 12))
            .put(Direction.UP, Block.createCuboidShape(4, 11, 4, 12, 16, 12))
            .put(Direction.DOWN, Block.createCuboidShape(4, 0, 4, 12, 5, 12))
    ).build();

    public PipeBlock(String itemName, int itemMaxStack, boolean hasLore, Settings settings)
    {
        super(itemName, itemMaxStack, hasLore, settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(NORTH_CONNECTION, PipeConnectionType.NONE)
                .with(EAST_CONNECTION, PipeConnectionType.NONE)
                .with(SOUTH_CONNECTION, PipeConnectionType.NONE)
                .with(WEST_CONNECTION, PipeConnectionType.NONE)
                .with(UP_CONNECTION, PipeConnectionType.NONE)
                .with(DOWN_CONNECTION, PipeConnectionType.NONE));

        for (BlockState state : this.getStateManager().getStates())
        {
            SHAPES.put(state, getShapeForState(state));
        }
    }

    public VoxelShape getShapeForState(BlockState state)
    {
        VoxelShape shape = Block.createCuboidShape(4, 4, 4, 12, 12, 12);
        for (Direction direction : Direction.values())
        {
            if (state.get(DIR_TO_CONNECTION.get(direction)) == PipeConnectionType.SIDE)
            {
                shape = VoxelShapes.union(shape, DIR_SHAPES.get(direction));
            }
        }
        return shape;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state)
    {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        return SHAPES.get(state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context)
    {
//        return VoxelShapes.cuboid(0f, 0f, 0f, 1f, 1.0f, 1f);
        return SHAPES.get(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx)
    {
        return this.getPlacementState(ctx.getWorld(), this.getDefaultState(), ctx.getBlockPos());
    }

    protected BlockState getPlacementState(BlockView world, BlockState state, BlockPos pos)
    {
        boolean bl = isNotConnected(state);
        state = this.getConnectedState(world, this.getDefaultState(), pos);
        if (bl && isNotConnected(state))
        {
            return state;
        }

//        boolean north = state.get(NORTH_CONNECTION);
//        boolean south = state.get(SOUTH_CONNECTION);
//        boolean east = state.get(EAST_CONNECTION);
//        boolean west = state.get(WEST_CONNECTION);
//        boolean up = state.get(UP_CONNECTION);
//        boolean down = state.get(DOWN_CONNECTION);
//        boolean nNS = !north && !south;
//        boolean nEW = !east && !west;
//        boolean nUD = !up && !down;
//        if (!west && nNS & nUD)
//        {
//            state = state.with(WEST_CONNECTION, true);
//        }
//        if (!east && nNS && nUD)
//        {
//            state = state.with(EAST_CONNECTION, true);
//        }
//        if (!north && nEW && nUD)
//        {
//            state = state.with(NORTH_CONNECTION, true);
//        }
//        if (!south && nEW && nUD)
//        {
//            state = state.with(SOUTH_CONNECTION, true);
//        }
////        System.out.println("up: " + up + ", nNS: " + nNS + ", nEW: " + nEW + ", nUD: " + nUD);
//        if (!up && nNS && nEW)
//        {
//            state = state.with(UP_CONNECTION, true);
//        }
//        if (!down && nNS && nEW)
//        {
//            state = state.with(DOWN_CONNECTION, true);
//        }
        return state;
    }

    @Override
    // TODO: Remove things from here.
    public void prepare(BlockState state, WorldAccess world, BlockPos pos, int flags, int maxUpdateDepth)
    {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Direction direction : Direction.Type.HORIZONTAL)
        {
            BlockState blockPos;
            boolean connection = state.get(DIR_TO_CONNECTION.get(direction)) == PipeConnectionType.SIDE;
            if (!connection || world.getBlockState(mutable.set(pos, direction)).isOf(this)) continue;
            mutable.move(Direction.DOWN);

            mutable.set(pos, direction).move(Direction.UP);
            blockPos = world.getBlockState(mutable);
            if (blockPos.isOf(Blocks.OBSERVER)) continue;
            BlockPos blockPos1 = mutable.offset(direction.getOpposite());
            BlockState blockState3 = blockPos.getStateForNeighborUpdate(direction.getOpposite(), world.getBlockState(blockPos1), world, mutable, blockPos1);
            RedstoneWireBlock.replace(blockPos, blockState3, world, mutable, flags, maxUpdateDepth);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos)
    {
        boolean connection = canConnectTo(neighborState, direction.getOpposite(), (World) world, neighborPos);

        // Already connected in direction
        if (connection && (state.get(DIR_TO_CONNECTION.get(direction)).isConnected()))
        {
            return state.with(DIR_TO_CONNECTION.get(direction), PipeConnectionType.SIDE);
        }

        return state.with(DIR_TO_CONNECTION.get(direction), PipeConnectionType.NONE);
    }

    // Only takes into account other pipes, connections to storages are enforced later.
    public boolean canConnectTo(BlockState state, Direction direction, World world, BlockPos pos)
    {
        if (state.getBlock() instanceof FluidAcceptor)
        {
            return ((FluidAcceptor) state.getBlock()).connectInDirection(state, direction);
        }
        return false;
    }

    protected static boolean isNotConnected(BlockState state)
    {
        return
                state.get(NORTH_CONNECTION) != PipeConnectionType.SIDE
                && state.get(SOUTH_CONNECTION) != PipeConnectionType.SIDE
                && state.get(EAST_CONNECTION) != PipeConnectionType.SIDE
                && state.get(WEST_CONNECTION) != PipeConnectionType.SIDE
                && state.get(UP_CONNECTION) != PipeConnectionType.SIDE
                && state.get(DOWN_CONNECTION) != PipeConnectionType.SIDE
                ;
    }

    protected static boolean isFullyConnected(BlockState state)
    {
        return state.get(NORTH_CONNECTION) == PipeConnectionType.SIDE
                && state.get(SOUTH_CONNECTION) == PipeConnectionType.SIDE
                && state.get(EAST_CONNECTION) == PipeConnectionType.SIDE
                && state.get(WEST_CONNECTION) == PipeConnectionType.SIDE
                && state.get(UP_CONNECTION) == PipeConnectionType.SIDE
                && state.get(DOWN_CONNECTION) == PipeConnectionType.SIDE
                ;
    }

    protected BlockState getConnectedState(BlockView world, BlockState state, BlockPos pos)
    {
        for (Direction direction : Direction.values())
        {
            PipeConnectionType property = state.get(DIR_TO_CONNECTION.get(direction));
            if (property == PipeConnectionType.SIDE) continue;
            BlockPos adjPos = pos.offset(direction);
            BlockState adjState = world.getBlockState(adjPos);
            state = state.with(DIR_TO_CONNECTION.get(direction), canConnectTo(adjState, direction.getOpposite(), (World) world, pos) ? PipeConnectionType.SIDE : PipeConnectionType.NONE);
        }
        return state;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved)
    {
        if (!state.isOf(newState.getBlock()))
        {
            removeStorageNodes(world, pos);
            world.removeBlockEntity(pos);
        }
    }

    public void removeStorageNodes(World world, BlockPos pos)
    {
        for (Direction direction : Direction.values())
        {
//            NodePos nodePos = new NodePos(pos.offset(direction), direction.getOpposite());
            NodePos nodePos = new NodePos(pos, direction);
            FluidNetwork.getInstance((ServerWorld) world).removeNode(world, nodePos);
        }
    }


    public void updateNetwork(World world, BlockPos pos, FluidNode node, boolean removed)
    {
        if (removed)
        {
            FluidNetwork.getInstance(world).removeNode(world, new NodePos(node));
        }
        else
        {
            FluidNetwork.getInstance(world).updateNode(world, new NodePos(node), node);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
    {
//        System.out.println(player.getStackInHand(hand));
        if (!player.getStackInHand(hand).isEmpty())
        {
            return ActionResult.PASS;
        }
        if (!world.isClient)
        {
            if (player.isSneaking())
            {
                System.out.println(FluidNetwork.getInstance(world).getNodes(pos));
                System.out.println("block entity: " + world.getBlockEntity(pos));
                return ActionResult.SUCCESS;
            }
            Direction direction = hit.getSide();

            Vec3d hitPos = hit.getPos();
            NMVec2f relative = NMMaths.removeAxis(direction.getAxis(), hitPos.subtract(pos.getX(), pos.getY(), pos.getZ()));
//            System.out.println(relative);

            Direction changeDirection = direction;
            if (!relative.isWithin(0.5f, 0.5f, 0.25f))
            {
                // X axis case
                if (relative.getY() > 0.75)
                    changeDirection = Direction.SOUTH;
                if (relative.getY() < 0.25)
                    changeDirection = Direction.NORTH;
                if (relative.getX() < 0.25)
                    changeDirection = Direction.DOWN;
                if (relative.getX() > 0.75)
                    changeDirection = Direction.UP;

                switch (direction.getAxis())
                {
                    case Y -> changeDirection = changeDirection.rotateClockwise(Direction.Axis.Z);
                    case Z -> changeDirection = NMMaths.swapDirections(changeDirection.rotateClockwise(Direction.Axis.Y));
                }
            }
            boolean connected = state.get(DIR_TO_CONNECTION.get(changeDirection)) == PipeConnectionType.SIDE;
            world.setBlockState(pos, state.with(DIR_TO_CONNECTION.get(changeDirection), connected ? PipeConnectionType.FORCED : PipeConnectionType.SIDE));

            return ActionResult.SUCCESS;
//        }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(NORTH_CONNECTION, EAST_CONNECTION, SOUTH_CONNECTION, WEST_CONNECTION, UP_CONNECTION, DOWN_CONNECTION);
    }

}
