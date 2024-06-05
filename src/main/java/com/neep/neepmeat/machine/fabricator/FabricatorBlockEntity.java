package com.neep.neepmeat.machine.fabricator;

import com.google.common.collect.Iterators;
import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.meatlib.inventory.ImplementedInventory;
import com.neep.meatlib.recipe.MeatlibRecipes;
import com.neep.meatlib.util.NbtSerialisable;
import com.neep.neepmeat.api.machine.MotorisedBlock;
import com.neep.neepmeat.machine.motor.MotorEntity;
import com.neep.neepmeat.transport.util.ItemPipeUtil;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FabricatorBlockEntity extends SyncableBlockEntity implements MotorisedBlock, ExtendedScreenHandlerFactory
{
    public static final Identifier CHANNEL_ID = new Identifier("fabricator_animation");

    private final List<BlockApiCache<Storage<ItemVariant>, Direction>> caches = Arrays.asList(new BlockApiCache[6]);
    private final FabricatorInventory inventory = new FabricatorInventory();
    private final FabricatorStorage storage = new FabricatorStorage();

    private float increment;
    private float progress;

    // Use on client only. Immediately set to false by instance.
    public boolean animation;

    public FabricatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    private boolean findMatching(int batchSize, Storage<ItemVariant> input, List<Ingredient> ingredients, List<ItemVariant> takenResources, TransactionContext transaction, Set<FabricatorStorage> visited) throws RecipeMatching.FabricatorLoopException
    {
        for (StorageView<ItemVariant> view : input)
        {
            // Recipe has been matched fully
            if (ingredients.isEmpty())
                return true;

            // Ignore empty views
            if (view.isResourceBlank() || view.getAmount() <= 0)
                continue;

            ItemVariant resource = view.getResource();

            var it = ingredients.iterator();
            while (it.hasNext())
            {
                Ingredient ingredient = it.next();

                if (ingredient.isEmpty())
                {
                    it.remove();
                    continue;
                }

                if (ingredient.test(view.getResource().toStack()))
                {
                    long extracted;
                    if (view instanceof FabricatorStorage fabricatorStorage)
                    {
                        ItemStack crafted = fabricatorStorage.getParent().craftRecursive(batchSize, transaction, visited);

                        if (crafted.getCount() == batchSize)
                        {
                            takenResources.add(ItemVariant.of(crafted));
                            it.remove();
                        }
                    }
                    else
                    {
                        // maxAmount = batchSize because crafting recipes only take 1 item for each ingredient.
                        extracted = view.extract(resource, batchSize, transaction);
                        if (extracted == batchSize)
                        {
                            takenResources.add(view.getResource());

                            // Remove the ingredient once it is satisfied
                            it.remove();
                        }
                    }
                }
            }
        }
        return ingredients.isEmpty();
    }

    private ItemStack craftRecursive(int batchSize, TransactionContext transaction, Set<FabricatorStorage> visited) throws RecipeMatching.FabricatorLoopException
    {
        if (visited.contains(storage))
            throw new RecipeMatching.FabricatorLoopException();

        visited.add(storage);

        return craft(batchSize, transaction, visited);
    }

    @Override
    public boolean motorTick(MotorEntity motor)
    {
        progress = Math.min(5, progress + increment);

        if (progress >= 5)
        {
            progress = 0;
            storage.updateRecipe();

            try
            {
                motorCraft();
            }
            catch (RecipeMatching.FabricatorLoopException ignored)
            {
            }
        }

        return true;
    }

    @Override
    public void setInputPower(float power)
    {
        this.increment = power;
    }

    private void motorCraft() throws RecipeMatching.FabricatorLoopException
    {
        CraftingRecipe recipe = getCurrentRecipe();
        if (recipe != null)
        {
            Direction facing = getCachedState().get(FabricatorBlock.FACING);
            Storage<ItemVariant> input = getInput(facing);

            // Mutable list of ingredients
            List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
            List<ItemVariant> takenResources = new ArrayList<>();

            try (Transaction transaction = Transaction.openOuter())
            {
                boolean foundAll = findMatching(1, input, ingredients, takenResources, transaction, new HashSet<>());
                if (!foundAll)
                {
                    transaction.abort();
                    return;
                }

                ItemStack result = recipe.getOutput(world.getRegistryManager());

                // ItemVariant.of shouldn't mutate the stack.
                long ejected = ItemPipeUtil.stackToAny((ServerWorld) world, pos, facing,
                        ItemVariant.of(result), result.getCount(), transaction);

                if (ejected != result.getCount())
                {
                    transaction.abort();
                    return;
                }

                // Eject remainders
                for (var taken : takenResources)
                {
                    // TODO: eject into input storage, not output
                    ItemStack remainder = taken.getItem().getRecipeRemainder(taken.toStack());
                    if (!remainder.isEmpty())
                    {
                        long ejected1 = ItemPipeUtil.stackToAny((ServerWorld) world, pos, facing,
                                ItemVariant.of(remainder), remainder.getCount(), transaction);

                        if (ejected1 != remainder.getCount())
                        {
                            transaction.abort();
                            return;
                        }
                    }
                }
                sendAnimation();
                transaction.commit();
                return;
            }
        }
        return;
    }

    private ItemStack craft(int batchSize, TransactionContext transaction, Set<FabricatorStorage> visited) throws RecipeMatching.FabricatorLoopException
    {
        CraftingRecipe recipe = getCurrentRecipe();
        if (recipe != null)
        {
            Direction facing = getCachedState().get(FabricatorBlock.FACING);
            Storage<ItemVariant> input = getInput(facing);

            // Mutable list of ingredients
            List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
            List<ItemVariant> takenResources = new ArrayList<>();

            try (Transaction inner = transaction.openNested())
            {
                boolean foundAll = findMatching(batchSize, input, ingredients, takenResources, inner, visited);

                if (!foundAll)
                {
                    inner.abort();
                    return ItemStack.EMPTY;
                }

                ItemStack result = recipe.getOutput(world.getRegistryManager()).copy();
                result.setCount(batchSize * result.getCount());

                // Eject remainders
                for (var taken : takenResources)
                {
                    // TODO: eject into input storage, not output
                    ItemStack remainder = taken.getItem().getRecipeRemainder(taken.toStack());
                    if (!remainder.isEmpty())
                    {
                        long ejected1 = ItemPipeUtil.stackToAny((ServerWorld) world, pos, facing,
                                ItemVariant.of(remainder), remainder.getCount(), inner);

                        if (ejected1 != remainder.getCount())
                        {
                            inner.abort();
                            return ItemStack.EMPTY;
                        }
                    }
                }
                inner.commit();
                sendAnimation();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }


    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        inventory.writeNbt(nbt);
        storage.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        inventory.readNbt(nbt);
        storage.readNbt(nbt);
    }

    private CombinedStorage<ItemVariant, Storage<ItemVariant>> getInput(Direction facing)
    {
        List<Storage<ItemVariant>> storages = new ArrayList<>();
        for (Direction direction : Direction.values())
        {
            if (direction == facing.getOpposite() || direction == facing.rotateYClockwise() || direction == facing.rotateYCounterclockwise())
            {
                Storage<ItemVariant> storage = findStorage(direction);
                if (storage != null)
                    storages.add(storage);
            }
        }
        return new CombinedStorage<>(storages);
    }

    @Nullable
    private Storage<ItemVariant> findStorage(Direction face)
    {
        if (caches.get(face.ordinal()) == null)
        {
            caches.set(face.ordinal(), BlockApiCache.create(ItemStorage.SIDED, (ServerWorld) world, pos.offset(face)));
        }

        return caches.get(face.ordinal()).find(face.getOpposite());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player)
    {
        return new FabricatorScreenHandler(playerInventory, inventory, syncId, this);
    }

    @Override
    public Text getDisplayName()
    {
        return Text.of("Enter Pattern");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf)
    {
        buf.writeBlockPos(getPos());
    }

    @Nullable
    public CraftingRecipe getCurrentRecipe()
    {
        storage.updateRecipe();
        return storage.getRecipe();
    }

    public Storage<ItemVariant> getStorage(Direction direction)
    {
        return direction == getCachedState().get(FabricatorBlock.FACING) ? storage : null;
    }

    public void sendAnimation()
    {
        if (world instanceof ServerWorld serverWorld)
        {
            PlayerLookup.tracking(this).forEach(p ->
            {
                // Not sure if you can send the same buf to multiple players.
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBlockPos(getPos());
                ServerPlayNetworking.send(p, CHANNEL_ID, buf);
            });
        }
    }

    public class FabricatorInventory implements ImplementedInventory, RecipeInputInventory
    {
        private final DefaultedList<ItemStack> items = DefaultedList.ofSize(9, ItemStack.EMPTY);

        @Override
        public DefaultedList<ItemStack> getItems()
        {
            return items;
        }

        @Override
        public void markDirty()
        {
            storage.updateRecipe();
            FabricatorBlockEntity.this.markDirty();
        }

        @Override
        public int getWidth()
        {
            return 3;
        }

        @Override
        public int getHeight()
        {
            return 3;
        }

        @Override
        public List<ItemStack> getInputStacks()
        {
            return items;
        }

        @Override
        public void provideRecipeInputs(RecipeMatcher finder)
        {
            for (ItemStack itemStack : this.items)
            {
                finder.addUnenchantedInput(itemStack);
            }
        }
    }

    public class FabricatorStorage extends SnapshotParticipant<ItemStack> implements Storage<ItemVariant>, StorageView<ItemVariant>, NbtSerialisable
    {
        private boolean needsLoading = true;

        private @Nullable CraftingRecipe recipe;

        private ItemStack bufferedStack = ItemStack.EMPTY; // Always real items produced from crafting.
        private ItemStack previewStack = ItemStack.EMPTY; // Always a preview of the craft.

        public FabricatorBlockEntity getParent()
        {
            return FabricatorBlockEntity.this;
        }

        public void updateRecipe()
        {
            recipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, inventory, world).orElse(null);

            if (recipe != null)
            {
                previewStack = recipe.getOutput(world.getRegistryManager()).copy();
            }
        }

        @Nullable
        public CraftingRecipe getRecipe()
        {
            return recipe;
        }

        @Override
        public boolean supportsInsertion()
        {
            return false;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            return 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            if (!bufferedStack.isEmpty() && resource.matches(bufferedStack) || resource.matches(previewStack))
            {
                long requiredExtra = maxAmount - bufferedStack.getCount();

                if (requiredExtra > 0)
                {
                    updateSnapshots(transaction);

                    int remainingCapacity = previewStack.getMaxCount() - bufferedStack.getCount();
                    int maxBatchSize = Math.min(remainingCapacity / previewStack.getCount(), 64);
                    int desiredBatchSize = (int) Math.ceil((double) requiredExtra / previewStack.getCount());

                    int batchSize = Math.min(maxBatchSize, desiredBatchSize);

                    // An exception seemed the best way to interrupt the recursion without loads of return value checking.
                    try
                    {
                        bufferedStack = craftRecursive(batchSize, transaction, new HashSet<>());

                        // If the batch craft failed, default to the result of a single craft.
                        // This should return a non-zero result when maxAmount = Long.MAX_VALUE as in StorageUtil::findExtractableResource.
                        if (batchSize > 1 && bufferedStack.isEmpty())
                        {
                            bufferedStack = craftRecursive(1, transaction, new HashSet<>());
                        }
                    }
                    catch (RecipeMatching.FabricatorLoopException ignored)
                    {
                        return 0;
                    }


                    int extractable = (int) Math.min(bufferedStack.getCount(), maxAmount);

                    if (extractable > 0)
                    {
                        bufferedStack.decrement(extractable);
                        return extractable;
                    }
                }
                else if (bufferedStack.isEmpty() && !previewStack.isEmpty()) // TODO: remove
                {
                    int amountToExtract = (int) Math.min(previewStack.getCount(), maxAmount);
                    if (amountToExtract > 0)
                    {
                        updateRecipe();

                        if (recipe != null)
                        {
                            updateSnapshots(transaction);
                            try
                            {
                                bufferedStack = craft(1, transaction, new HashSet<>());
                            }
                            catch (RecipeMatching.FabricatorLoopException e)
                            {
                                return 0;
                            }

                            int extractable = Math.min(bufferedStack.getCount(), amountToExtract);

                            if (extractable == amountToExtract)
                            {
                                bufferedStack.decrement(amountToExtract);
                                return extractable;
                            }
                        }
                    }
                }
                else if (!bufferedStack.isEmpty())
                {
                    int amountToExtract = (int) Math.min(bufferedStack.getCount(), maxAmount);
                    if (amountToExtract > 0)
                    {
                        updateSnapshots(transaction);

                        bufferedStack.decrement(amountToExtract);

                        return amountToExtract;
                    }
                }
            }
            return 0;
        }

        @Override
        public boolean isResourceBlank()
        {
            if (needsLoading)
                load();

            return bufferedStack.isEmpty() && previewStack.isEmpty();
        }

        @Override
        public ItemVariant getResource()
        {
            if (needsLoading)
                load();

            if (!bufferedStack.isEmpty())
                return ItemVariant.of(bufferedStack);
            else
                return ItemVariant.of(previewStack);
        }

        @Override
        public long getAmount()
        {
            if (needsLoading)
                load();

            if (!bufferedStack.isEmpty())
                return bufferedStack.getCount();
            else
                return previewStack.getCount();
        }

        @Override
        public long getCapacity()
        {
            if (needsLoading)
                load();

            if (!bufferedStack.isEmpty())
                return bufferedStack.getCount();
            else
                return previewStack.getCount();
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator()
        {
            if (needsLoading)
                load();

            return Iterators.singletonIterator(this);
        }

        private void load()
        {
            needsLoading = false;
            updateRecipe();
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt)
        {
            if (recipe != null)
                nbt.putString("recipe", recipe.getId().toString());

            nbt.put("buffered", bufferedStack.writeNbt(new NbtCompound()));

            return nbt;
        }

        @Override
        public void readNbt(NbtCompound nbt)
        {
            if (nbt.contains("recipe"))
                this.recipe = MeatlibRecipes.getInstance().getVanilla(RecipeType.CRAFTING, Identifier.tryParse(nbt.getString("recipe"))).orElse(null);
            else
                this.recipe = null;

            this.bufferedStack = ItemStack.fromNbt(nbt.getCompound("buffered"));
        }

        @Override
        protected ItemStack createSnapshot()
        {
            return bufferedStack.copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot)
        {
            this.bufferedStack = snapshot.copy();
        }

        @Override
        protected void onFinalCommit()
        {
            markDirty();
        }

    }
}
