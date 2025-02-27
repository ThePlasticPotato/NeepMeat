package com.neep.neepmeat.compat.emi;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.neep.meatlib.recipe.MeatlibRecipes;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.api.processing.BlockCrushingRegistry;
import com.neep.neepmeat.compat.emi.helper.LazyEmiRecipeCategory;
import com.neep.neepmeat.compat.emi.recipe.*;
import com.neep.neepmeat.datagen.tag.NMTags;
import com.neep.neepmeat.init.NMBlocks;
import com.neep.neepmeat.init.NMItems;
import com.neep.neepmeat.init.NMrecipeTypes;
import com.neep.neepmeat.init.ScreenHandlerInit;
import com.neep.neepmeat.machine.live_machine.LivingMachines;
import com.neep.neepmeat.plc.PLCBlocks;
import com.neep.neepmeat.plc.recipe.PLCRecipes;
import com.neep.neepmeat.plc.recipe.TransformingToolRecipe;
import com.neep.neepmeat.recipe.AdvancedBlockCrushingRecipe;
import com.neep.neepmeat.recipe.BlockCrushingRecipe;
import com.neep.neepmeat.transport.FluidTransport;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;

public class NMEmiPlugin implements EmiPlugin {
    public static final EmiStack ALLOY_SMELTING_WORKSTATION = EmiStack.of(NMBlocks.ALLOY_KILN);
    public static final EmiStack COMPACTING_WORKSTATION = EmiStack.of(NMBlocks.CHARNEL_COMPACTOR);
    public static final EmiStack ENLIGHTENING_WORKSTATION = EmiStack.of(NMBlocks.PEDESTAL);
    public static final EmiStack INTEGRATOR_WORKSTATION = EmiStack.of(NMBlocks.INTEGRATOR_EGG);
    public static final EmiStack ADV_INTEGRATOR_WORKSTATION = EmiStack.of(NMBlocks.ADVANCED_INTEGRATOR);
    public static final EmiStack GRINDING_WORKSTATION = EmiStack.of(NMBlocks.CRUSHER);
    public static final EmiStack LARGE_CRUSHER_WORKSTATION = EmiStack.of(NMBlocks.LARGE_CRUSHER);
    public static final EmiStack CRUSHER_SEGMENT_WORKSTATION = EmiStack.of(LivingMachines.CRUSHER_SEGMENT);
    public static final EmiStack VIVISECTION_WORKSTATION = EmiStack.of(NMItems.SACRIFICIAL_SCALPEL);
    public static final EmiStack HEATING_WORKSTATION = EmiStack.of(FluidTransport.MULTI_TANK);
    public static final EmiStack MANUFACTURE_WORKSTATION = EmiStack.of(PLCBlocks.PLC);
    public static final EmiStack MIXING_WORKSTATION = EmiStack.of(NMBlocks.MIXER);
    public static final EmiStack PRESSING_WORKSTATION = EmiStack.of(NMBlocks.HYDRAULIC_PRESS);
    public static final EmiStack SURGERY_WORKSTATION = EmiStack.of(PLCBlocks.PLC);
    public static final EmiStack TROMMEL_WORKSTATION = EmiStack.of(NMBlocks.SMALL_TROMMEL);
    public static final EmiStack LARGE_TROMMEL_WORKSTATION = EmiStack.of(LivingMachines.LARGE_TROMMEL);

    public static final EmiRecipeCategory ALLOY_SMELTING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/alloy_smelting"), ALLOY_SMELTING_WORKSTATION);
    public static final EmiRecipeCategory COMPACTING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/compacting"), COMPACTING_WORKSTATION);
    public static final EmiRecipeCategory ENLIGHTENING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/enlightening"), ENLIGHTENING_WORKSTATION);
    public static final EmiRecipeCategory GRINDING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/grinding"), GRINDING_WORKSTATION);
    public static final EmiRecipeCategory ADVANCED_CRUSHING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/advanced_crushing"), LARGE_CRUSHER_WORKSTATION);
    public static final EmiRecipeCategory BLOCK_CRUSHING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/block_crushing"), GRINDING_WORKSTATION);
    public static final EmiRecipeCategory ADVANCED_BLOCK_CRUSHING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/advanced_block_crushing"), LARGE_CRUSHER_WORKSTATION);
    public static final EmiRecipeCategory VIVISECTION = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/vivisection"), VIVISECTION_WORKSTATION);
    public static final EmiRecipeCategory HEATING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/heating"), HEATING_WORKSTATION);
    public static final EmiRecipeCategory ITEM_MANUFACTURE = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/manufacture"), MANUFACTURE_WORKSTATION);
    public static final EmiRecipeCategory ENTITY_TO_ITEM_MANUFACTURE = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/entity_to_item_manufacture"), MANUFACTURE_WORKSTATION);
    public static final EmiRecipeCategory TRANSFORMING_TOOL = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/transforming_tool"), MANUFACTURE_WORKSTATION);
    public static final EmiRecipeCategory MIXING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/mixing"), MIXING_WORKSTATION);
    public static final EmiRecipeCategory PRESSING = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/pressing"), PRESSING_WORKSTATION);
    public static final EmiRecipeCategory SURGERY = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/surgery"), SURGERY_WORKSTATION);
    public static final EmiRecipeCategory TROMMEL = new LazyEmiRecipeCategory(new Identifier(NeepMeat.NAMESPACE, "plugins/trommel"), TROMMEL_WORKSTATION);

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ALLOY_SMELTING);
        registry.addCategory(COMPACTING);
        registry.addCategory(ENLIGHTENING);
        registry.addCategory(GRINDING);
        registry.addCategory(ADVANCED_CRUSHING);
        registry.addCategory(BLOCK_CRUSHING);
        registry.addCategory(ADVANCED_BLOCK_CRUSHING);
        registry.addCategory(VIVISECTION);
        registry.addCategory(HEATING);
        registry.addCategory(ITEM_MANUFACTURE);
        registry.addCategory(ENTITY_TO_ITEM_MANUFACTURE);
        registry.addCategory(MIXING);
        registry.addCategory(PRESSING);
        registry.addCategory(SURGERY);
        registry.addCategory(TRANSFORMING_TOOL);
        registry.addCategory(TROMMEL);

        registry.addWorkstation(ALLOY_SMELTING, ALLOY_SMELTING_WORKSTATION);
        registry.addWorkstation(COMPACTING, COMPACTING_WORKSTATION);
        registry.addWorkstation(ENLIGHTENING, ENLIGHTENING_WORKSTATION);
        registry.addWorkstation(ENLIGHTENING, INTEGRATOR_WORKSTATION);
        registry.addWorkstation(ENLIGHTENING, ADV_INTEGRATOR_WORKSTATION);
        registry.addWorkstation(GRINDING, GRINDING_WORKSTATION);
        registry.addWorkstation(GRINDING, LARGE_CRUSHER_WORKSTATION);
        registry.addWorkstation(GRINDING, CRUSHER_SEGMENT_WORKSTATION);
        registry.addWorkstation(ADVANCED_CRUSHING, LARGE_CRUSHER_WORKSTATION);
        registry.addWorkstation(ADVANCED_CRUSHING, CRUSHER_SEGMENT_WORKSTATION);
        registry.addWorkstation(BLOCK_CRUSHING, CRUSHER_SEGMENT_WORKSTATION);
        registry.addWorkstation(BLOCK_CRUSHING, GRINDING_WORKSTATION);
        registry.addWorkstation(ADVANCED_BLOCK_CRUSHING, CRUSHER_SEGMENT_WORKSTATION);
        registry.addWorkstation(ADVANCED_BLOCK_CRUSHING, GRINDING_WORKSTATION);
        registry.addWorkstation(VIVISECTION, VIVISECTION_WORKSTATION);
        registry.addWorkstation(HEATING, HEATING_WORKSTATION);
        registry.addWorkstation(ITEM_MANUFACTURE, MANUFACTURE_WORKSTATION);
        registry.addWorkstation(ENTITY_TO_ITEM_MANUFACTURE, MANUFACTURE_WORKSTATION);
        registry.addWorkstation(TRANSFORMING_TOOL, MANUFACTURE_WORKSTATION);
        registry.addWorkstation(MIXING, MIXING_WORKSTATION);
        registry.addWorkstation(PRESSING, PRESSING_WORKSTATION);
        registry.addWorkstation(SURGERY, SURGERY_WORKSTATION);
        registry.addWorkstation(TROMMEL, TROMMEL_WORKSTATION);
        registry.addWorkstation(TROMMEL, LARGE_TROMMEL_WORKSTATION);

        RecipeManager manager = registry.getRecipeManager();
        manager.listAllOfType(NMrecipeTypes.ENLIGHTENING)
                .stream()
                .map(EnlighteningEmiRecipe::new)
                .forEach(registry::addRecipe);
        manager.listAllOfType(NMrecipeTypes.MIXING)
                .stream()
                .map(MixingEmiRecipe::new)
                .forEach(registry::addRecipe);
        manager.listAllOfType(NMrecipeTypes.PRESSING)
                .stream()
                .map(PressingEmiRecipe::new)
                .forEach(registry::addRecipe);

        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.ALLOY_SMELTING)
                .map(AlloySmeltingEmiRecipe::new)
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.GRINDING)
                .filter(r -> !r.destroy())
                .filter(r -> !(r instanceof BlockCrushingRecipe))
                .map(r -> new GrindingEmiRecipe(GRINDING, r))
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.ADVANCED_CRUSHING)
                .filter(r -> !r.destroy())
                .filter(r -> !(r instanceof BlockCrushingRecipe))
                .map(r -> new GrindingEmiRecipe(ADVANCED_CRUSHING, r))
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.HEATING)
                .map(HeatingEmiRecipe::new)
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(PLCRecipes.MANUFACTURE)
                .map(ItemManufactureEmiRecipe::new)
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(PLCRecipes.ENTITY_TO_ITEM)
                .map(EntityToItemManufactureEmiRecipe::new)
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.TROMMEL)
                .map(TrommelEmiRecipe::new)
                .forEach(registry::addRecipe);
        MeatlibRecipes.getInstance().getAllValuesOfType(NMrecipeTypes.VIVISECTION)
                .map(VivisectionEmiRecipe::new)
                .forEach(registry::addRecipe);

        registry.addRecipe(new ToolManufactureEmiRecipe(TransformingToolRecipe.getInstance()));

        // Charnel Compactor recipes
        int page = 0;
        UnmodifiableIterator<List<Item>> iterator = Iterators.partition(Registries.ITEM.getEntryList(NMTags.CHARNEL_COMPACTOR).orElseThrow().stream().map(RegistryEntry::value).iterator(), 35);
        while (iterator.hasNext()) {
            List<Item> entries = iterator.next();
            registry.addRecipe(new CompactingEmiRecipe(entries, NMItems.CRUDE_INTEGRATION_CHARGE, page++));
        }

        registry.addRecipe(new VivisectionEmiRecipe(NMBlocks.INTEGRATOR_EGG.asItem(), NMItems.CHRYSALIS));

       BlockCrushingRecipe crushingRecipe;
       crushingRecipe = BlockCrushingRecipe.get(registry.getRecipeManager());
        if (crushingRecipe != null)
            registry.addRecipe(new BlockCrushingEmiRecipe(BLOCK_CRUSHING, crushingRecipe, BlockCrushingRegistry.INSTANCE::getBasicEntries));

        crushingRecipe = AdvancedBlockCrushingRecipe.get(registry.getRecipeManager());
        if (crushingRecipe != null)
            registry.addRecipe(new BlockCrushingEmiRecipe(ADVANCED_BLOCK_CRUSHING, crushingRecipe, BlockCrushingRegistry.INSTANCE::getAdvancedEntries));

        // Recipe Handlers
        registry.addRecipeHandler(ScreenHandlerInit.FABRICATOR, new FabricatorRecipeHandler());
    }
}
