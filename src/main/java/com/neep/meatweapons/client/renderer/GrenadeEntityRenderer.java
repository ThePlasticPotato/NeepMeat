package com.neep.meatweapons.client.renderer;

import com.neep.meatweapons.MeatWeapons;
import com.neep.meatweapons.client.MWClient;
import com.neep.meatweapons.client.model.CannonBulletEntityModel;
import com.neep.meatweapons.entity.BounceGrenadeEntity;
import com.neep.meatweapons.entity.CannonBulletEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class GrenadeEntityRenderer extends EntityRenderer<BounceGrenadeEntity>
{
    public EntityModel<CannonBulletEntity> model;

    public GrenadeEntityRenderer(EntityRendererFactory.Context context)
    {
        super(context);
        model = new CannonBulletEntityModel(context.getPart(MWClient.MODEL_CANNON_BULLET_LAYER));
    }

    @Override
    public void render(BounceGrenadeEntity cannonBulletEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
//        matrixStack.push();
//        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(g, cannonBulletEntity.prevYaw, cannonBulletEntity.getYaw()) - 90.0F));
//        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(g, cannonBulletEntity.prevPitch, cannonBulletEntity.getPitch())));
//        model.render(matrixStack, vertexConsumerProvider.getBuffer(model.getLayer(getTexture(cannonBulletEntity))), i, 1, 1.0F, 1.0F, 1.0F, 1F);
//        matrixStack.pop();
    }

    @Override
    public Identifier getTexture(BounceGrenadeEntity entity)
    {
        // TODO: change
        return new Identifier(MeatWeapons.NAMESPACE, "textures/entity/explosive_shell.png");
    }
}
