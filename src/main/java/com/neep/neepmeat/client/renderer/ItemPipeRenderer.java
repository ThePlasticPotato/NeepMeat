package com.neep.neepmeat.client.renderer;

import com.neep.neepmeat.transport.block.item_transport.entity.ItemPipeBlockEntity;
import com.neep.neepmeat.transport.item_network.ItemInPipe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(value = EnvType.CLIENT)
public class ItemPipeRenderer<T extends ItemPipeBlockEntity> implements BlockEntityRenderer<T>
{

    public ItemPipeRenderer(BlockEntityRendererFactory.Context context)
    {
    }

    @Override
    public void render(T be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);

        int count = 0;
        for (ItemInPipe item : be.getItems())
        {
            // Proctection against item rendering insanity
            if (count > 20)
                break;

            count++;

            ItemStack stack = item.getItemStack();
            matrices.push();

            long diff = be.getWorld().getTime() - item.tickStart;
            float progress = (diff + tickDelta) * item.speed;
            Vec3d pos = item.getPosition(progress);

            matrices.translate(pos.x, pos.y, pos.z);
            matrices.scale(0.4f, 0.4f, 0.4f);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (Math.PI / 2)));
            renderer.renderItem(stack, ModelTransformationMode.FIXED, light, overlay, matrices, vertexConsumers, null, 0);

            matrices.pop();
        }

        matrices.pop();
    }
}