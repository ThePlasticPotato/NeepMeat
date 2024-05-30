package com.neep.neepmeat.compat.rei;

import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.compat.rei.display.*;
import com.neep.neepmeat.plc.recipe.EntityToItemRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;

public interface NMREIPlugin
{
    CategoryIdentifier<GrindingDisplay> GRINDING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/grinding");
    CategoryIdentifier<GrindingDisplay> ADVANCED_CRUSHING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/advanced_crushing");
    CategoryIdentifier<CompactingDisplay> COMPACTING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/compacting");
    CategoryIdentifier<MixingDisplay> MIXING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/mixing");
    CategoryIdentifier<AlloySmeltingDisplay> ALLOY_SMELTING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/alloy_smelting");
    CategoryIdentifier<VivisectionDisplay> VIVISECTION = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/vivisection");
    CategoryIdentifier<EnlighteningDisplay> ENLIGHTENING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/enlightening");
    CategoryIdentifier<PressingDisplay> PRESSING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/pressing");
    CategoryIdentifier<SurgeryDisplay> SURGERY = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/surgery");

    CategoryIdentifier<ItemManufactureDisplay> MANUFACTURE = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/item_manufacture");
    CategoryIdentifier<EntityToItemDisplay> ENTITY_TO_ITEM = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/entity_to_item_manufacture");

    CategoryIdentifier<TrommelDisplay> TROMMEL = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/trommel");
    CategoryIdentifier<HeatingDisplay> HEATING = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/heating");
    CategoryIdentifier<TransformingToolDisplay> TRANSFORMING_TOOL = CategoryIdentifier.of(NeepMeat.NAMESPACE, "plugins/transforming_tool");
}
