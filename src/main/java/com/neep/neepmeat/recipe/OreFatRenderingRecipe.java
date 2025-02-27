package com.neep.neepmeat.recipe;

import com.google.gson.JsonObject;
import com.neep.meatlib.recipe.ImplementedRecipe;
import com.neep.meatlib.recipe.ingredient.RecipeInput;
import com.neep.meatlib.recipe.ingredient.RecipeInputs;
import com.neep.meatlib.recipe.ingredient.RecipeOutputImpl;
import com.neep.neepmeat.api.processing.OreFatRegistry;
import com.neep.neepmeat.init.NMrecipeTypes;
import com.neep.neepmeat.machine.crucible.CrucibleStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class OreFatRenderingRecipe extends ImplementedRecipe<CrucibleStorage>
{
    protected Identifier id;
    protected RecipeInput<Fluid> fluidInput;
    protected RecipeOutputImpl<Fluid> fluidOutput;

    public OreFatRenderingRecipe(Identifier id, RecipeInput<Fluid> fluidInput, RecipeOutputImpl<Fluid> fluidOutput)
    {
        this.fluidInput = fluidInput;
        this.fluidOutput = fluidOutput;
        this.id = id;
    }

    @Override
    public boolean matches(CrucibleStorage inventory, World world)
    {
        return fluidInput.test(inventory.getStorage(null))
                && OreFatRegistry.getFromInput(inventory.getItemStorage(null).getResource().getItem()) != null;
    }

    @Override
    public boolean fits(int width, int height)
    {
        return false;
    }

    public RecipeInput<Fluid> getFluidInput()
    {
        return fluidInput;
    }

    public RecipeOutputImpl<Fluid> getFluidOutput()
    {
        return fluidOutput;
    }

    @Override
    public Identifier getId()
    {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return NMrecipeTypes.ORE_FAT_RENDERING_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType()
    {
        return NMrecipeTypes.ORE_FAT_RENDERING;
    }

    public Item takeInputs(CrucibleStorage storage, int itemAmount, TransactionContext transaction)
    {
        SingleSlotStorage<ItemVariant> itemStorage = storage.getItemStorage(null);
        Storage<FluidVariant> fluidStorage = storage.getStorage(null);
        OreFatRegistry.Entry entry = OreFatRegistry.getFromInput(itemStorage.getResource().getItem());
        Item item = itemStorage.getResource().getItem();

        try (Transaction take = transaction.openNested())
        {
            Optional<Fluid> fluid = fluidInput.getFirstMatching(fluidStorage, take);
            if (entry == null || fluid.isEmpty())
                return null;

            long ex1 = itemStorage.extract(ItemVariant.of(item), itemAmount, take);
            long ex2 = fluidStorage.extract(FluidVariant.of(fluid.get()), fluidInput.amount() * itemAmount, take);
            if (ex1 == itemAmount && ex2 == fluidInput.amount() * itemAmount)
            {
                take.commit();
            }
            else
            {
                take.abort();
                return null;
            }
        }

        try (Transaction eject = transaction.openNested())
        {
            boolean bl1 = true;
            fluidOutput.setNbt(entry.nbt());
            FluidVariant outputVariant = FluidVariant.of(fluidOutput.resource(), entry.nbt());
            for (int i = 0; i < itemAmount; ++i)
            {
                bl1 = bl1 && storage.getFluidOutput().insert(outputVariant,
                        FluidConstants.INGOT * fluidOutput.randomAmount(entry.renderingYield() - 1),
                        eject) > 0;
            }
            fluidOutput.setNbt(null);

            if (bl1)
            {
                eject.commit();
                return item;
            }
            else eject.abort();
        }
        return null;
    }

    public static class Serializer implements RecipeSerializer<OreFatRenderingRecipe>
    {
        RecipeFactory<OreFatRenderingRecipe> factory;

        public Serializer(RecipeFactory<OreFatRenderingRecipe> recipeFactory)
        {
            this.factory = recipeFactory;
        }

        @Override
        public OreFatRenderingRecipe read(Identifier id, JsonObject json)
        {
            JsonObject fluidInputElement = JsonHelper.getObject(json, "fluid_input");
            RecipeInput<Fluid> fluidInput = RecipeInput.fromJsonRegistry(RecipeInputs.FLUID, fluidInputElement);

            JsonObject outputElement = JsonHelper.getObject(json, "output");
            RecipeOutputImpl<Fluid> fluidOutput = RecipeOutputImpl.fromJsonRegistry(Registries.FLUID, outputElement);

            return this.factory.create(id, fluidInput, fluidOutput);
        }

        @Override
        public OreFatRenderingRecipe read(Identifier id, PacketByteBuf buf)
        {
            RecipeInput<Fluid> fluidInput = RecipeInput.fromBuffer(buf);
            RecipeOutputImpl<Fluid> fluidOutput = RecipeOutputImpl.fromBuffer(Registries.FLUID, buf);

            return this.factory.create(id, fluidInput, fluidOutput);
        }

        @Override
        public void write(PacketByteBuf buf, OreFatRenderingRecipe recipe)
        {
            recipe.fluidInput.write(buf);
            recipe.fluidOutput.write(Registries.FLUID, buf);
        }

        @FunctionalInterface
        public interface RecipeFactory<T extends OreFatRenderingRecipe>
        {
            T create(Identifier var1, RecipeInput<Fluid> in2, RecipeOutputImpl<Fluid> out);
        }
    }
}
