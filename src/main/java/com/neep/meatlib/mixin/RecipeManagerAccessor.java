package com.neep.meatlib.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor
{
    @Accessor
    Map<RecipeType<?>, Map<Identifier, Recipe<?>>> getRecipes();

    @Accessor
    Map<Identifier, Recipe<?>> getRecipesById();

    @Invoker
    <C extends Inventory, T extends Recipe<C>> Map<Identifier, T> callGetAllOfType(RecipeType<T> type);
}
