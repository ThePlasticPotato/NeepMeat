package com.neep.neepmeat.client.renderer;

import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.client.model.GenericModel;
import com.neep.neepmeat.entity.bovine_horror.BovineHorrorEntity;
import com.neep.neepmeat.util.SightUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BovineHorrorRenderer extends GeoEntityRenderer<BovineHorrorEntity>
{
    public BovineHorrorRenderer(EntityRendererFactory.Context renderManager)
    {
        super(renderManager, new GenericModel<>(NeepMeat.NAMESPACE,
                "geo/bovine_horror.geo.json",
                "textures/entity/bovine_horror.png",
                "animations/bovine_horror.animation.json"
                ));

        this.shadowRadius = 1.5F;
    }

    @Override
    public void render(BovineHorrorEntity animatable, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight)
    {
        animatable.prevVisibility = (float) MathHelper.lerp(0.1, animatable.prevVisibility, animatable.getVisibility());

        poseStack.push();
//        if (animatable.isPhase2())
//        {
//            Vec3d vel = animatable.getVelocity();
//            double hlen = vel.horizontalLength();
//
//            Vector3f hor = new Vector3f((float) vel.x, 0, (float) vel.z);
//            hor.normalize();
//            hor.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(90));
//            poseStack.multiply(hor.getDegreesQuaternion((float) (hlen * 40)));
//        }

        super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }

    @Override
    public Color getRenderColor(BovineHorrorEntity animatable, float partialTick, int packedLight)
    {
        float alpha = animatable.prevVisibility;
        if (SightUtil.canPlayerSee(MinecraftClient.getInstance().player))
        {
            alpha = 1;
        }
        return Color.ofRGBA(1, 1, 1, alpha);
    }

    @Override
    public RenderLayer getRenderType(BovineHorrorEntity animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick)
    {
        if (SightUtil.canPlayerSee(MinecraftClient.getInstance().player))
        {
            return RenderLayer.getEntityCutout(texture);
        }
        return RenderLayer.getEntityTranslucent(texture);
    }

    @Override
    protected void applyRotations(BovineHorrorEntity animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick)
    {
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - rotationYaw));
    }
}
