package com.neep.meatweapons.client.renderer.meatgun;

import com.neep.meatweapons.client.MWExtraModels;
import com.neep.meatweapons.item.meatgun.DoubleCarouselModule;
import com.neep.meatweapons.item.meatgun.MeatgunComponent;
import com.neep.meatweapons.item.meatgun.TripleCarouselModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class DoubleCarouselModuleRenderer implements MeatgunModuleRenderer<DoubleCarouselModule>
{
    private final ItemRenderer itemRenderer;

    public DoubleCarouselModuleRenderer(MinecraftClient client)
    {
        this.itemRenderer = client.getItemRenderer();
    }

    @Override
    public void render(ItemStack stack, MeatgunComponent component, DoubleCarouselModule module, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        BakedModel base = itemRenderer.getModels().getModelManager().getModel(MWExtraModels.MEATGUN_DOUBLE_CAROUSEL);
        renderItem(stack, mode, false, matrices, vertexConsumers, light, overlay, base);
    }
}
