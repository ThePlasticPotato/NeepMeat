package com.neep.neepmeat.transport.block.fluid_transport;

import com.neep.meatlib.item.ItemSettings;
import com.neep.neepmeat.init.NMBlockEntities;
import com.neep.neepmeat.transport.FluidTransport;
import com.neep.neepmeat.transport.api.pipe.AbstractPipeBlock;
import com.neep.neepmeat.transport.api.pipe.FluidPipe;
import com.neep.neepmeat.transport.fluid_network.PipeConnectionType;
import com.neep.neepmeat.transport.fluid_network.node.BlockPipeVertex;
import com.neep.neepmeat.transport.machine.fluid.FluidPipeBlockEntity;
import com.neep.neepmeat.util.MiscUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.object.Color;

@SuppressWarnings("UnstableApiUsage")
public class FluidPipeBlock extends AbstractPipeBlock implements BlockEntityProvider, FluidPipe
{
    public final PipeCol col;

    public FluidPipeBlock(String itemName, FluidPipe.PipeCol col, ItemSettings itemSettings, Settings settings)
    {
        super(itemName, itemSettings, settings);
        this.col = col;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved)
    {
        FluidPipe.onStateReplaced(world, pos, state, newState, this);

        super.onStateReplaced(state, world, pos, newState, moved);

        if (world.isClient())
            return;

        if (!state.isOf(newState.getBlock()))
        {
            removePipe((ServerWorld) world, state, pos);
//            updateNeighbourPipes(world, pos, state);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify)
    {
        BlockPos subtracted = fromPos.subtract(pos);
        Direction direction = Direction.fromVector(subtracted.getX(), subtracted.getY(), subtracted.getZ());
        BlockState nextState = getStateForNeighborUpdate(state, direction, world.getBlockState(fromPos), world, pos, fromPos);

        // Block state change must be applied to the world in order for PipeNetwork::discoverNodes to pick it up
        world.setBlockState(pos, nextState, Block.NOTIFY_LISTENERS);

        BlockState fromState = world.getBlockState(fromPos);

        // Update comnnections: junction change, update is from another pipe

        // Neighbour state changes have already been applied, so there is no way of knowing if the update came from a
        // destroyed pipe.
        boolean foundPipe = FluidPipe.findFluidPipe(world, fromPos, fromState) != null;
        boolean nodeChange = false;
        if (!foundPipe)
        {
            // If the nodes have changed, we need to update the pipe.
            nodeChange = createStorageNodes(world, pos, nextState);
//            {
//                FluidPipeBlockEntity.find(world, pos).ifPresent(be -> be.updateConnectionChange(state, nextState));
//            }
        }
//        else if (!state.equals(nextState))
//        {
//            // The addition of a pipe can change this one's junction status
//        }
        if (foundPipe || nodeChange || !state.equals(nextState))
        {
            FluidPipeBlockEntity.find(world, pos).ifPresent(be -> be.updateConnectionChange(state, nextState));
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack)
    {
        BlockState updatedState = enforceApiConnections(world, pos, state);
        world.setBlockState(pos, updatedState, Block.NOTIFY_ALL);
        if (!world.isClient())
        {
            createStorageNodes(world, pos, updatedState);

            FluidPipeBlockEntity.find(world, pos).ifPresent(be -> be.updateHiddenConnections(updatedState));
//            updateNeighbourPipes(world, pos, state);
        }
    }

    @Override
    public void onConnectionUpdate(World world, BlockState state, BlockState newState, BlockPos pos, PlayerEntity entity)
    {
        if (world.isClient())
            return;

        createStorageNodes(world, pos, newState);
        FluidPipeBlockEntity.find(world, pos).ifPresent(be -> be.updateConnectionChange(state, newState));
//        FluidPipeBlockEntity.find(world, pos).ifPresent(be -> be.updateHiddenConnections(newState));
//        updateNeighbourPipes(world, pos, state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos)
    {
        if (state.get(WATERLOGGED))
        {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        PipeConnectionType type = state.get(DIR_TO_CONNECTION.get(direction));
        boolean forced = type == PipeConnectionType.FORCED;
        boolean otherConnected = false;

        boolean canConnect = canConnectTo(neighborState, direction.getOpposite(), (World) world, neighborPos);
        if (world instanceof ServerWorld serverWorld && !(neighborState.getBlock() instanceof FluidPipeBlock))
        {

            canConnect = canConnect || (canConnectApi((World) world, pos, state, direction));
        }

        // Check if neighbour is forced
        if (neighborState.getBlock() instanceof FluidPipeBlock)
        {
            forced = forced || neighborState.get(DIR_TO_CONNECTION.get(direction.getOpposite())) == PipeConnectionType.FORCED;
            otherConnected = neighborState.get(DIR_TO_CONNECTION.get(direction.getOpposite())) == PipeConnectionType.SIDE;

        }

        // AAAAAAAAAAAAAAAAAAAA
        PipeConnectionType finalConnection =
                otherConnected ? PipeConnectionType.SIDE :
                        forced ? PipeConnectionType.FORCED
                                : canConnect ? PipeConnectionType.SIDE : PipeConnectionType.NONE;


        return state.with(DIR_TO_CONNECTION.get(direction), finalConnection);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
    {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.STICK))
        {
            if (!world.isClient() && world.getBlockEntity(pos) instanceof FluidPipeBlockEntity<?> be)
            {
                if (be.getPipeVertex() instanceof BlockPipeVertex vertex && !vertex.canSimplify())
                {
                    player.sendMessage(Text.of("Amount: " + vertex.getAmount() + ", Pump height: " + vertex.getPumpHeight() + ", Total height: " + vertex.getTotalHeight()));
//                    System.out.println(vertex.getAmount());
//                    System.out.println(vertex.getVariant());
//                    System.out.println(vertex.getPumpHeight());
//                    System.out.println(vertex.getTotalHeight());
                }

//                Collection<FluidNode> nodes = FluidNodeManager.getInstance(world).getNodes(pos);
//                FluidNode node = nodes.iterator().next();
//                node.hasPump = true;
            }
            return ActionResult.SUCCESS;
        }
        else if (stack.getItem() instanceof DyeItem dyeItem)
        {
            PipeCol newCol = PipeCol.get(dyeItem.getColor());
            if (newCol != col)
            {
                FluidPipeBlock newPipe = FluidTransport.COLOURED_FLUID_PIPES.get(newCol);
                world.setBlockState(pos, newPipe.getStateWithProperties(state));
                world.playSound(player, pos, SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1, 1);
                return ActionResult.SUCCESS;
            }
            else
            {
                return ActionResult.FAIL;
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return NMBlockEntities.FLUID_PIPE.instantiate(pos, state);
    }

    @Override
    public PipeCol getCol(World world, BlockPos pos, BlockState blockState)
    {
        return col;
    }

    // Only takes into account other pipes, connections to storages are enforced later.
    @Override
    public boolean canConnectTo(BlockState toState, Direction toFace, World world, BlockPos toPos)
    {
        if (toState.getBlock() instanceof FluidPipe otherPipe)
        {
            return col.matches(otherPipe.getCol(world, toPos, toState))
                    && otherPipe.connectInDirection(world, toPos, toState, toFace);
        }
        return false;
    }

    // Creates blockstate connections to fluid containers after placing
    private BlockState enforceApiConnections(World world, BlockPos pos, BlockState state)
    {
        if (!world.isClient)
        {
            BlockState state2 = state;
            for (Direction direction : Direction.values())
            {
                if (canConnectApi(world, pos, state, direction))
                {
                    state2 = state2.with(DIR_TO_CONNECTION.get(direction), PipeConnectionType.SIDE);
                }
            }
            return state2;
        }
        return state;
    }

    private boolean canConnectApi(World world, BlockPos pos, BlockState state, Direction direction)
    {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, pos.offset(direction), direction.getOpposite());
        return storage != null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return MiscUtil.checkType(type, NMBlockEntities.FLUID_PIPE, ((world1, pos, state1, blockEntity) -> blockEntity.tick()), null, world);
    }

    @Environment(value= EnvType.CLIENT)
    public static int getTint(BlockState state, BlockRenderView world, BlockPos pos, int index)
    {
        if (index == 1 && state.getBlock() instanceof FluidPipeBlock fluidPipeBlock)
        {
            int col = fluidPipeBlock.col.hexCode();
            Color first = Color.ofOpaque(col);
            Color second = Color.ofOpaque(0x844b2f);

            int d = 120;
            int d1 = 255 - d;
            Color mixed = Color.ofRGB(
                    (d * first.getRed() + d1 * second.getRed()) / 255,
                    (d * first.getGreen() + d1 * second.getGreen()) / 255,
                    (d * first.getBlue() + d1 * second.getBlue()) / 255
            );
            return mixed.getColor();
        }
        return 0xFFFFFF;
    }

    @Environment(value= EnvType.CLIENT)
    public static int getItemTint(ItemStack stack, int i)
    {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FluidPipeBlock fluidPipeBlock)
        {
            return fluidPipeBlock.col.hexCode();
//            Color first = Color.ofOpaque(col);
//            Color second = Color.ofOpaque(0x844b2f);
//
//            int d = 120;
//            int d1 = 255 - d;
//            Color mixed = Color.ofRGB(
//                    (d * first.getRed() + d1 * second.getRed()) / 255,
//                    (d * first.getGreen() + d1 * second.getGreen()) / 255,
//                    (d * first.getBlue() + d1 * second.getBlue()) / 255
//            );
//            return mixed.getColor();
        }
        return 0xFFFFFF;
    }
}