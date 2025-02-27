package com.neep.meatweapons.particle;

import com.neep.meatweapons.MeatWeapons;
import com.neep.meatweapons.client.BeamRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

public class BulletTrailEffect extends BeamGraphicsEffect
{
    public static final Identifier TRAIL_TEXTURE = new Identifier(MeatWeapons.NAMESPACE, "textures/misc/bullet_trail.png");
    public static final RenderLayer TRAIL_LAYER = RenderLayer.getEntityTranslucent(TRAIL_TEXTURE);

    public BulletTrailEffect(World world, UUID uuid, PacketByteBuf buf)
    {
        super(world, uuid, buf);
    }

    @Override
    public void tick()
    {
        super.tick();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, VertexConsumerProvider consumers, float tickDelta)
    {
        matrices.push();
        VertexConsumer consumer = consumers.getBuffer(TRAIL_LAYER);
        float x = (maxTime - time + 2 - tickDelta) / (float) maxTime;
        BeamRenderer.renderBeam(matrices, consumer, camera.getPos(),
                start, end, (int) col.x, (int) col.y, (int) col.z,
               255, scale, 255);
        matrices.pop();

//        214, 175, 32,
    }
}
