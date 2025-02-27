package com.neep.neepmeat.machine.surgical_controller;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.box.ImmutableBox;
import com.neep.neepmeat.client.NMExtraModels;
import com.neep.neepmeat.client.plc.PLCHudRenderer;
import com.neep.neepmeat.plc.block.entity.PLCBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(value = EnvType.CLIENT)
public class PLCInstance extends BlockEntityInstance<PLCBlockEntity> implements DynamicInstance
{
    private final ModelData robotModel;
    private final MatrixStack matrixStack = new MatrixStack();
    private final int animOffset;

    public PLCInstance(MaterialManager materialManager, PLCBlockEntity blockEntity)
    {
        super(materialManager, blockEntity);
        matrixStack.translate(getInstancePosition().getX(), getInstancePosition().getY(), getInstancePosition().getZ());

        robotModel = materialManager.defaultCutout().material(Materials.TRANSFORMED)
                .getModel(NMExtraModels.P_PLC_ROBOT).createInstance();

        this.animOffset = (int) (Math.random() * 360);
    }

    @Override
    public ImmutableBox getVolume()
    {
        return super.getVolume();
    }

    @Override
    protected void remove()
    {
        robotModel.delete();
    }

    @Override
    public void beginFrame()
    {
        var robot = blockEntity.getSurgeryRobot();
        robot.prevX = robot.clientX;
        robot.prevY = robot.clientY;
        robot.prevZ = robot.clientZ;

        // Smooth the robot's position
        robot.clientX = MathHelper.lerp(0.1d, robot.clientX, robot.getX());
        robot.clientY = MathHelper.lerp(0.1d, robot.clientY, robot.getY());
        robot.clientZ = MathHelper.lerp(0.1d, robot.clientZ, robot.getZ());
        robot.clientYaw = MathHelper.lerpAngleDegrees(0.1f, robot.clientYaw, robot.getYaw());

        double vx = robot.clientX - robot.prevX;
        double vy = robot.clientY - robot.prevY;
        double vz = robot.clientZ - robot.prevZ;

        double speed = (float) Math.sqrt(vx * vx + vz * vz);

        // Only render the robot in 3rd person
        PLCHudRenderer plcHudRenderer = PLCHudRenderer.getInstance();
        if (plcHudRenderer != null && plcHudRenderer.getBlockEntity() == blockEntity)
        {
            robotModel.loadIdentity().scale(0, 0, 0);
        }
        else
        {
            matrixStack.push();
            matrixStack.translate(
                    robot.clientX - blockEntity.getPos().getX() - 0.5,
                    robot.clientY - blockEntity.getPos().getY() - 0.5,
                    robot.clientZ - blockEntity.getPos().getZ() - 0.5);

            float tickDelta = AnimationTickHolder.getPartialTicks();
            int time = (int) ((blockEntity.getWorld().getTime() % 360) + animOffset);
            double sinTime = Math.sin(Math.toRadians(time * 7)) * Math.cos(Math.toRadians(tickDelta) * 7)
                    + Math.cos(Math.toRadians(time * 7)) * Math.sin(Math.toRadians(tickDelta) * 7);
            matrixStack.translate(0, 0.5 + 0.1 * sinTime, 0);

            matrixStack.translate(0.5, 0.5, 0.5);
            matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(robot.clientYaw + 180));
            matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees((float) (100 * speed)));
            matrixStack.translate(-0.5, -0.5, -0.5);

            robotModel.setTransform(matrixStack);

            matrixStack.pop();
        }
    }

    @Override
    public void updateLight()
    {
        relight(getInstancePosition(), robotModel);
    }
}