package com.neep.neepmeat.client.renderer;

import com.neep.neepmeat.client.NMExtraModels;
import com.neep.neepmeat.transport.machine.item.ItemPumpBlock;
import com.neep.neepmeat.transport.machine.item.ItemPumpBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;


@Environment(value = EnvType.CLIENT)
public class ItemPumpRenderer implements BlockEntityRenderer<ItemPumpBlockEntity>
{
    public ItemPumpRenderer(BlockEntityRendererFactory.Context context)
    {

    }

    @Override
    public void render(ItemPumpBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        BakedModelManager manager = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelManager();
        BakedModel shuttle = BakedModelManagerHelper.getModel(manager, NMExtraModels.ITEM_PUMP_SHUTTLE);
        BakedModel chest = BakedModelManagerHelper.getModel(manager, NMExtraModels.ITEM_PUMP_CHEST);
        BlockModelRenderer renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();

        Direction facing = be.getCachedState().get(ItemPumpBlock.FACING);
        Vector3f vec = be.getCachedState().get(ItemPumpBlock.FACING).getUnitVector();
        World world = be.getWorld();

        matrices.push();

        matrices.translate(0.5, 0.5, 0.5);
//        float offset = (float) Math.sin((world.getTime() + tickDelta) / 10);

        be.offset = (float) MathHelper.lerp(0.3, be.offset, be.shuttle > 0 ? (float) 0.2 : 0);

        vec.mul((float) be.offset, (float) be.offset, (float) be.offset);
        matrices.translate(vec.x, vec.y, vec.z);

        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(facing.asRotation()));
        if (facing == Direction.DOWN)
        {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90));
        }
        else if (facing == Direction.UP)
        {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
        }

        matrices.translate(-0.5, -0.5, -0.5);
        renderer.render(
                be.getWorld(),
                shuttle,
                be.getCachedState(),
                be.getPos(),
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getCutout()),
                true,
                Random.create(),
                0,
                0
        );

        if (world.getBlockEntity(be.getPos().offset(facing.getOpposite())) instanceof Inventory chest1)
        {
            renderer.render(
                    be.getWorld(),
                    chest,
                    be.getCachedState(),
                    be.getPos(),
                    matrices,
                    vertexConsumers.getBuffer(RenderLayer.getCutout()),
                    true,
                    Random.create(),
                    0,
                    0
            );
        }
        matrices.pop();
    }
}
