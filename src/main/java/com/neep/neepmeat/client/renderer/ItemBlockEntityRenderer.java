package com.neep.neepmeat.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.function.Function;

@Environment(value = EnvType.CLIENT)
public class ItemBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T>
{
    private final Function<T, ItemStack> stackFunction;
    private final Function<T, Float> offsetFunction;

    public ItemBlockEntityRenderer(BlockEntityRendererFactory.Context ctx, Function<T, ItemStack> function, Function<T, Float> offset)
    {
        this.stackFunction = function;
        this.offsetFunction = offset;
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {

        ItemStack stack = stackFunction.apply(entity);
        if (stack.isEmpty())
            return;

        matrices.push();

        matrices.translate(0.5, offsetFunction.apply(entity), 0.5);
        // Wrap degrees to ensure precision for long-lived worlds
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((MathHelper.wrapDegrees(entity.getWorld().getTime()) + tickDelta) * 1));

        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, null, 0);

        matrices.pop();
    }
}
