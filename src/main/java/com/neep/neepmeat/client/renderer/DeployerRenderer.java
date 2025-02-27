package com.neep.neepmeat.client.renderer;

import com.neep.meatlib.block.BaseFacingBlock;
import com.neep.neepmeat.client.NMExtraModels;
import com.neep.neepmeat.machine.deployer.DeployerBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class DeployerRenderer implements BlockEntityRenderer<DeployerBlockEntity>
{
    public DeployerRenderer(BlockEntityRendererFactory.Context context)
    {
    }

    @Override
    public void render(DeployerBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        matrices.push();

        ItemStack stack = be.getResource().toStack((int) be.getAmount());

        // Offset shuttle while powered
        be.shuttleOffset = (float) MathHelper.lerp(0.3, be.shuttleOffset, be.powered ? (float) 0.1 : 0);

        matrices.push();
        Direction facing = be.getCachedState().get(BaseFacingBlock.FACING);
        BERenderUtils.rotateFacing(facing, matrices);
        matrices.translate(0, 0, -be.shuttleOffset);
        BERenderUtils.renderModel(NMExtraModels.DEPLOYER_SHUTTLE, matrices, be.getWorld(), be.getPos(), be.getCachedState(), vertexConsumers);

        float scale = 0.4f + be.shuttleOffset;
        if (stack.getItem() instanceof BlockItem blockItem)
        {
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(scale, scale, scale);
            matrices.translate(-0.5, -0.5, -0.5);

            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(blockItem.getBlock().getDefaultState(),
                    matrices,
                    vertexConsumers,
                    light,
                    overlay);
        }
        else
        {
            matrices.translate(0.5, 0.4, 0.5);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, null, 0);
        }
        matrices.pop();


        matrices.pop();
    }
}