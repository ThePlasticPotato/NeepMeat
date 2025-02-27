package com.neep.neepmeat.implant.item;

import com.neep.neepmeat.NeepMeat;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

/**
 * An implant installer that operates on an ItemStack.
 */
public interface ItemImplantInstaller
{
    SimpleRegistry<ItemImplantInstaller> REGISTRY = FabricRegistryBuilder.createSimple(
            ItemImplantInstaller.class, new Identifier(NeepMeat.NAMESPACE, "item_implant_installer")).buildAndRegister();

    void install(ItemStack stack);
}
