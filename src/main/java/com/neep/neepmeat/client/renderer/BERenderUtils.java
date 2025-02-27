package com.neep.neepmeat.client.renderer;

import com.neep.neepmeat.init.NMFluids;
import com.neep.neepmeat.mixin.BlockModelRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public class BERenderUtils
{
    public static void renderModelSmooth(Identifier model, MatrixStack matrices, World world, BlockPos pos, BlockState state, VertexConsumerProvider vertexConsumers)
    {
        BakedModelManager manager = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelManager();
        BakedModel handle = BakedModelManagerHelper.getModel(manager, model);
        BlockModelRenderer renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();

        renderer.renderSmooth(world, handle, state, pos, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), false, Random.create(), 0, 0);
//        renderFlat(
//                renderer,
//                world,
//                handle,
//                state,
//                pos,
//                matrices,
//                vertexConsumers.getBuffer(RenderLayer.getCutout()),
//                true,
//                Random.create(),
//                0,
//                0
//        );
    }

    public static void renderModel(Identifier model, MatrixStack matrices, World world, BlockPos pos, BlockState state, VertexConsumerProvider vertexConsumers)
    {
        BakedModelManager manager = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelManager();
        BakedModel handle = manager.getModel(model);
        BlockModelRenderer renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();

        renderFlat(
                renderer,
                world,
                handle,
                state,
                pos,
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getCutout()),
                true,
                Random.create(),
                0,
                0
        );
    }

    private static void renderFlat(BlockModelRenderer renderer, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, net.minecraft.util.math.random.Random random, long seed, int overlay)
    {
        BitSet bitSet = new BitSet(3);
        random.setSeed(seed);
        List<BakedQuad> list2 = model.getQuads(state, null, random);
        if (!list2.isEmpty())
        {
            renderQuadsFlat(renderer, world, state, pos, -1, overlay, true, matrices, vertexConsumer, list2, bitSet);
        }
    }

    private static void renderQuadsFlat(BlockModelRenderer renderer, BlockRenderView world, BlockState state, BlockPos pos, int light, int overlay, boolean useWorldLight, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet flags)
    {
        BlockModelRendererAccessor accessor = (BlockModelRendererAccessor) renderer;
        for (BakedQuad bakedQuad : quads)
        {
            if (useWorldLight)
            {
                light = WorldRenderer.getLightmapCoordinates(world, state, pos);
            }

            float f = 1; // What is this?
            accessor.callRenderQuad(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad, f, f, f, f, light, light, light, light, overlay);
        }
    }

    public static Direction nearestDirection(Vector3f vec)
    {
        Direction closest = Direction.NORTH;
        float closestDist = 100;
        for (Direction direction : Direction.values())
        {
            Vector3f vec1 = closest.getUnitVector();
            vec1.sub(vec);
            float len = vec1.length();
            if (len < closestDist)
            {
                closest = direction;
                closestDist = len;
            }
        }
        return Direction.UP;
    }

    /**
     * @param facing   The JSON model must be facing North by default.
     * @param matrices Remember to push and pop.
     */
    public static void rotateFacing(Direction facing, MatrixStack matrices)
    {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing)
        {
            case SOUTH:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                break;
            }
            case WEST:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                break;
            }
            case NORTH:
            {
                break;
            }
            case EAST:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                break;
            }
            case DOWN:
            {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                break;
            }
            case UP:
            {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                break;
            }
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }

    // For models that are facing the wrong direction
    public static void rotateFacingSouth(Direction facing, MatrixStack matrices)
    {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing)
        {
            case NORTH:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                break;
            }
            case EAST:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                break;
            }
            case SOUTH:
            {
                break;
            }
            case WEST:
            {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                break;
            }
            case UP:
            {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                break;
            }
            case DOWN:
            {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                break;
            }
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }

    public static void renderFluidCuboid(VertexConsumerProvider vcp, MatrixStack matrices, FluidVariant fluid, float startY, float endY, float depth, float scaleY, int light)
    {
        if (NMFluids.isGas(fluid))
        {
            renderGas(vcp, matrices, fluid, startY, endY, depth, scaleY, light);
            return;
        }

        Sprite sprite = FluidVariantRendering.getSprite(fluid);
        VertexConsumer consumer = vcp.getBuffer(RenderLayer.getTranslucent());
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        int col = FluidVariantRendering.getColor(fluid);

        float r = ((col >> 16) & 255) / 256f;
        float g = ((col >> 8) & 255) / 256f;
        float b = (col & 255) / 256f;

        if (fluid.isBlank() || scaleY == 0)
        {
            return;
        }

        float dist = startY + (endY - startY) * scaleY;
        if (FluidVariantAttributes.isLighterThanAir(fluid))
        {
            matrices.translate(1, 1, 0);
            matrices.scale(-1, -1, 1);
        }

        QuadEmitter emitter = renderer.meshBuilder().getEmitter();

        for (Direction direction : Direction.values())
        {
            if (direction.getAxis().isVertical())
            {
//                emitter.square(Direction.UP, depth, depth, 1 - depth, 1 - depth, 1 - dist);
                emitter.square(direction, depth, depth, 1 - depth, 1 - depth, direction == Direction.UP ? 1 - dist : startY);
            }
            else if (direction != Direction.DOWN)
            {
                emitter.square(direction, depth, startY, 1 - depth, dist, depth);
            }

            emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
            emitter.spriteColor(0, -1, -1, -1, -1);
            consumer.quad(matrices.peek(), emitter.toBakedQuad(0, sprite, false), r, g, b, light, OverlayTexture.DEFAULT_UV);
        }
    }

    public static void renderGas(VertexConsumerProvider vcp, MatrixStack matrices, FluidVariant fluid, float startY, float endY, float depth, float scaleY, int light)
    {
        Sprite sprite = FluidVariantRendering.getSprite(fluid);
        VertexConsumer consumer = vcp.getBuffer(RenderLayer.getTranslucent());
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        int col = FluidVariantRendering.getColor(fluid);

        float r = ((col >> 16) & 255) / 256f;
        float g = ((col >> 8) & 255) / 256f;
        float b = (col & 255) / 256f;

        if (fluid.isBlank() || scaleY == 0)
            return;

        QuadEmitter emitter = renderer.meshBuilder().getEmitter();

        for (Direction direction : Direction.values())
        {
            if (direction.getAxis().isVertical())
            {
                emitter.square(direction, depth, depth, 1 - depth, 1 - depth, direction == Direction.UP ? 1 - endY : startY);
            }
            else if (direction != Direction.DOWN)
            {
                emitter.square(direction, depth, startY, 1 - depth, endY, depth);
            }

            float alpha = MathHelper.clamp(scaleY, 0, 1);
            int colWithAlpha = 0x00FFFFFF | (((int) (alpha * 255)) << 24);
            emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
            quad(consumer, matrices.peek(), emitter.toBakedQuad(0, sprite, false), 1, r, g, b, alpha, light);
        }
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float brightness, float red, float green, float blue, float alpha, int light)
    {
        Vector3f unit = quad.getFace().getUnitVector();
        Matrix4f matrix4f = matrixEntry.getPositionMatrix();
        Vector3f vector3f = matrixEntry.getNormalMatrix().transform(unit);

        int[] js = quad.getVertexData();
        int j = js.length / 8;

        try (MemoryStack memoryStack = MemoryStack.stackPush())
        {
            ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSizeByte());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int k = 0; k < j; ++k)
            {
                intBuffer.clear();
                intBuffer.put(js, k * 8, 8);
                float f = byteBuffer.getFloat(0);
                float g = byteBuffer.getFloat(4);
                float h = byteBuffer.getFloat(8);
                float o = brightness * red;
                float p = brightness * green;
                float q = brightness * blue;
                float s = brightness * alpha;

                float m = byteBuffer.getFloat(16);
                float n = byteBuffer.getFloat(20);
                Vector4f vector4f = matrix4f.transform(new Vector4f(f, g, h, 1.0F));
                consumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), o, p, q, s, m, n, OverlayTexture.DEFAULT_UV, light, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }
}
