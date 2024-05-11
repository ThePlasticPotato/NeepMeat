package com.neep.neepmeat.entity.scutter;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class FarmingScutter extends ScutterEntity
{
    private final LinkedHashSet<BlockPos> targets = new LinkedHashSet<>(); // Preserve order

    public FarmingScutter(EntityType<? extends ScutterEntity> type, World world)
    {
        super(type, world);
    }

    public static boolean isGrownCrop(BlockState state)
    {
        return state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMature(state);
    }

    public static boolean isFarmland(BlockState state)
    {
        return state.isOf(Blocks.FARMLAND);
    }

    @Override
    protected void initGoals()
    {
        super.initGoals();
        goalSelector.add(1, new ScutterBreakCropGoal(this));
        goalSelector.add(2, new ScutterMoveToCropGoal(this, 0.3f, 32));
        goalSelector.add(3, new ScutterFindGrownCropsGoal(this, 20));
        goalSelector.add(4, new ScutterFindFarmlandGoal(this, 7, 1));
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand)
    {
        if (player.isSneaking())
        {
            remove(RemovalReason.DISCARDED);
            return ActionResult.SUCCESS;
        }
        return super.interactAt(player, hitPos, hand);
    }

    @Override
    public void tick()
    {
        super.tick();

//        if (!getWorld().isClient())
//            goalSelector.tick();

        targets.removeIf(p -> !isGrownCrop(getWorld().getBlockState(p)));

        tickMovement();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state)
    {
    }

    @Override
    public boolean canTakeDamage()
    {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount)
    {
        return false;
    }

    @Override
    protected boolean shouldFollowLeash()
    {
        return false;
    }

    public void addTargets(Collection<BlockPos> nearest)
    {
        targets.addAll(nearest);
    }

    public Set<BlockPos> getTargets()
    {
        return targets;
    }
}
