package com.neep.neepmeat.compat.rei;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.neep.meatlib.recipe.MeatRecipeType;
import com.neep.meatlib.recipe.MeatlibRecipe;
import com.neep.neepmeat.api.processing.BlockCrushingRegistry;
import com.neep.neepmeat.compat.rei.category.*;
import com.neep.neepmeat.compat.rei.display.*;
import com.neep.neepmeat.datagen.tag.NMTags;
import com.neep.neepmeat.init.NMBlocks;
import com.neep.neepmeat.init.NMItems;
import com.neep.neepmeat.init.NMrecipeTypes;
import com.neep.neepmeat.machine.live_machine.LivingMachines;
import com.neep.neepmeat.machine.mixer.MixingRecipe;
import com.neep.neepmeat.plc.PLCBlocks;
import com.neep.neepmeat.plc.recipe.EntityToItemRecipe;
import com.neep.neepmeat.plc.recipe.ItemManufactureRecipe;
import com.neep.neepmeat.plc.recipe.PLCRecipes;
import com.neep.neepmeat.plc.recipe.TransformingToolRecipe;
import com.neep.neepmeat.recipe.*;
import com.neep.neepmeat.transport.FluidTransport;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class NMClientPlugin implements REIClientPlugin, NMREIPlugin
{
//    private static final Comparator<MeatlibRecipe<?>> RECIPE_COMPARATOR = Comparator.comparing((MeatlibRecipe<?> o) -> o.getId().getNamespace()).thenComparing(o -> o.getId().getPath());
//    private List<MeatlibRecipe<?>> sortedRecipes = null;
//    DisplayAdditionReason SPECIAL_RECIPE_MANAGER = DisplayAdditionReason.simple();

    @Override
    public void registerDisplays(DisplayRegistry registry)
    {
        registerRecipeFiller(registry, ItemManufactureRecipe.class, PLCRecipes.MANUFACTURE, ItemManufactureDisplay::new);
        registerRecipeFiller(registry, EntityToItemRecipe.class, PLCRecipes.ENTITY_TO_ITEM, EntityToItemDisplay::new);
        registry.add(new TransformingToolDisplay(TransformingToolRecipe.getInstance()));
        registerRecipeFiller(registry, CrushingRecipe.class, NMrecipeTypes.GRINDING, GrindingDisplay::isValid, GrindingDisplay.filler(GRINDING));
        registerRecipeFiller(registry, AdvancedCrushingRecipe.class, NMrecipeTypes.ADVANCED_CRUSHING, GrindingDisplay::isValid, GrindingDisplay.filler(ADVANCED_CRUSHING));
        registerRecipeFiller(registry, BlockCrushingRecipe.class, NMrecipeTypes.GRINDING, e -> true,
                BlockCrushingDisplay.filler(BLOCK_CRUSHING, BlockCrushingRegistry.INSTANCE::getBasicEntries));
        registerRecipeFiller(registry, AdvancedBlockCrushingRecipe.class, NMrecipeTypes.ADVANCED_CRUSHING,
                BlockCrushingDisplay.filler(ADVANCED_BLOCK_CRUSHING, BlockCrushingRegistry.INSTANCE::getAdvancedEntries));
        registerRecipeFiller(registry, NormalTrommelRecipe.class, NMrecipeTypes.TROMMEL, TrommelDisplay::new);
        registerRecipeFiller(registry, FluidHeatingRecipe.class, NMrecipeTypes.HEATING, HeatingDisplay::new);
        registerRecipeFiller(registry, AlloyKilnRecipe.class, NMrecipeTypes.ALLOY_SMELTING, AlloySmeltingDisplay::new);
        registry.registerRecipeFiller(MixingRecipe.class, NMrecipeTypes.MIXING, MixingDisplay::new);
        registry.registerRecipeFiller(EnlighteningRecipe.class, NMrecipeTypes.ENLIGHTENING, EnlighteningDisplay::new);
        registry.registerRecipeFiller(PressingRecipe.class, NMrecipeTypes.PRESSING, PressingDisplay::new);
        registerRecipeFiller(registry, VivisectionRecipe.class, NMrecipeTypes.VIVISECTION, VivisectionDisplay::new);

        // Charnel Compactor recipes
        int page = 0;
        UnmodifiableIterator<List<Item>> iterator = Iterators.partition(Registries.ITEM.getEntryList(NMTags.CHARNEL_COMPACTOR).orElseThrow().stream().map(RegistryEntry::value).iterator(), 35);
        while (iterator.hasNext())
        {
            List<Item> entries = iterator.next();
            registry.add(CompactingDisplay.of(entries, Collections.singletonList(EntryIngredients.of(new ItemStack(NMItems.CRUDE_INTEGRATION_CHARGE))), page++));
        }

        // Vivisection
//        registry.add(VivisectionDisplay.of(List.of(EntityType.ZOMBIE), Collections.singletonList(EntryIngredients.of(new ItemStack(NMItems.REANIMATED_HEART)))));
        registry.add(VivisectionDisplay.of(NMBlocks.INTEGRATOR_EGG.asItem(), Collections.singletonList(EntryIngredients.of(new ItemStack(NMItems.CHRYSALIS)))));
    }

    @Override
    public void registerCategories(CategoryRegistry registry)
    {
        registry.add(
                new ItemManufactureCategory(),
                new EntityToItemManufactureCategory(),
                new TransformingToolCategory(),
                new GrindingCategory(),
                new AdvancedCrushingCategory(),
                new BlockCrushingCategory(BLOCK_CRUSHING, EntryStacks.of(NMBlocks.CRUSHER)),
                new BlockCrushingCategory(ADVANCED_BLOCK_CRUSHING, EntryStacks.of(LivingMachines.CRUSHER_SEGMENT)),
                new TrommelCategory(),
                new HeatingCategory(),
                new CompactingCategory(),
                new MixingCategory(),
                new AlloyKilnCategory(),
                new VivisectionCategory(),
                new EnlighteningCategory(),
                new PressingCategory()
        );

        registry.addWorkstations(ITEM_MANUFACTURE, EntryStacks.of(PLCBlocks.PLC.asItem()));
        registry.addWorkstations(ENTITY_TO_ITEM, EntryStacks.of(PLCBlocks.PLC.asItem()));
        registry.addWorkstations(TRANSFORMING_TOOL, EntryStacks.of(PLCBlocks.PLC.asItem()));
        registry.addWorkstations(GRINDING,
                EntryStacks.of(NMBlocks.CRUSHER.asItem()),
                EntryStacks.of(NMBlocks.LARGE_CRUSHER.asItem()),
                EntryStacks.of(LivingMachines.CRUSHER_SEGMENT.asItem()));
        registry.addWorkstations(ADVANCED_CRUSHING,
                EntryStacks.of(NMBlocks.LARGE_CRUSHER.asItem()),
                EntryStacks.of(LivingMachines.CRUSHER_SEGMENT.asItem()));
        registry.addWorkstations(BLOCK_CRUSHING,
                EntryStacks.of(NMBlocks.CRUSHER.asItem()),
                EntryStacks.of(NMBlocks.LARGE_CRUSHER.asItem()),
                EntryStacks.of(LivingMachines.CRUSHER_SEGMENT.asItem()));
        registry.addWorkstations(ADVANCED_BLOCK_CRUSHING,
                EntryStacks.of(NMBlocks.LARGE_CRUSHER.asItem()),
                EntryStacks.of(LivingMachines.CRUSHER_SEGMENT.asItem()));
        registry.addWorkstations(TROMMEL, EntryStacks.of(NMBlocks.SMALL_TROMMEL.asItem()));
        registry.addWorkstations(TROMMEL, EntryStacks.of(LivingMachines.LARGE_TROMMEL.asItem()));
        registry.addWorkstations(HEATING, EntryStacks.of(FluidTransport.MULTI_TANK.asItem()));
        registry.addWorkstations(COMPACTING, EntryStacks.of(NMBlocks.CHARNEL_COMPACTOR.asItem()));
        registry.addWorkstations(MIXING, EntryStacks.of(NMBlocks.MIXER.asItem()));
        registry.addWorkstations(ALLOY_SMELTING, EntryStacks.of(NMBlocks.ALLOY_KILN.asItem()));
        registry.addWorkstations(VIVISECTION, EntryStacks.of(NMItems.SACRIFICIAL_SCALPEL.asItem()));
        registry.addWorkstations(ENLIGHTENING, EntryStacks.of(NMBlocks.PEDESTAL.asItem()));
        registry.addWorkstations(PRESSING, EntryStacks.of(NMBlocks.HYDRAULIC_PRESS.asItem()));
    }

    private ReloadStage lastStage;

    @Override
    public void postStage(PluginManager<REIClientPlugin> manager, ReloadStage stage)
    {
        // This method is executed twice for START and twice for END. To prevent all recipes from being injected twice,
        // it must detect a state that only occurs once.
        if (stage == ReloadStage.END && lastStage == ReloadStage.START)
        {
            // Inject all Meatlib recipes
//            this.sortedRecipes = MeatRecipeManager.getInstance().values().parallelStream().sorted(RECIPE_COMPARATOR).collect(Collectors.toList());
//            for (int i = sortedRecipes.size() - 1; i >= 0; i--)
//            {
//                MeatlibRecipe<?> recipe = sortedRecipes.get(i);
//                DisplayRegistry.getInstance().addWithReason(recipe, SPECIAL_RECIPE_MANAGER);
//            }
        }
        lastStage = stage;
    }

    public static <T extends MeatlibRecipe<?>, D extends Display> void registerRecipeFiller(DisplayRegistry registry, Class<T> typeClass, MeatRecipeType<? super T> recipeType, Function<? extends T, @Nullable D> filler) {
        registerRecipeFiller(registry, typeClass, recipeType, t -> true, filler);
    }

    public static <T extends MeatlibRecipe<?>, D extends Display> void registerRecipeFiller(DisplayRegistry registry, Class<T> typeClass, MeatRecipeType<? super T> recipeType, Predicate<? extends T> predicate, Function<? extends T, @Nullable D> filler) {
        registerRecipeFiller(registry, typeClass, type -> Objects.equals(recipeType, type), predicate, filler);
    }

    public static <T extends MeatlibRecipe<?>, D extends Display> void registerRecipeFiller(DisplayRegistry registry, Class<T> typeClass, Predicate<MeatRecipeType<? super T>> type, Predicate<? extends T> predicate, Function<? extends T, @Nullable D> filler)
    {
        registry.registerFiller(typeClass, recipe -> type.test((MeatRecipeType<? super T>) recipe.getType()) && ((Predicate<T>) predicate).test(recipe), filler);
    }

}