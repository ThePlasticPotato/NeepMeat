package com.neep.meatweapons.item;

import com.neep.meatlib.item.MeatlibItemSettings;
import com.neep.meatweapons.MWItems;
import com.neep.meatweapons.client.model.HeavyCannonItemModel;
import com.neep.meatweapons.entity.ExplodingShellEntity;
import com.neep.meatweapons.network.MWAttackC2SPacket;
import com.neep.neepmeat.init.NMSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.model.GeoModel;

public class HeavyCannonItem extends BaseGunItem
{
    public String controllerName = "controller";

    public HeavyCannonItem()
    {
        super("heavy_cannon", MWItems.BALLISTIC_CARTRIDGE, 1, 15,false, new MeatlibItemSettings());
        this.sounds.put(GunSounds.FIRE_PRIMARY, NMSounds.HAND_CANNON_FIRE);
        this.sounds.put(GunSounds.RELOAD, NMSounds.HAND_CANNON_RELOAD);
    }

    @Override
    public UseAction getUseAction(ItemStack stack)
    {
        return UseAction.NONE;
    }

    @Override
    protected GeoModel<? extends BaseGunItem> createModel()
    {
        return new HeavyCannonItemModel();
    }

    @Override
    public Vec3d getMuzzleOffset(LivingEntity entity, ItemStack stack)
    {
        return new Vec3d(entity.getMainHandStack().equals(stack) ? -0.2 : 0.2,
                entity.isSneaking() ? -0.15 : 0.1,
                0);
    }

    @Override
    public void trigger(World world, PlayerEntity player, ItemStack stack, int id, double pitch, double yaw, MWAttackC2SPacket.HandType handType)
    {
        if (!player.getItemCooldownManager().isCoolingDown(this))
        {
            if (stack.getDamage() != this.maxShots)
            {
                player.getItemCooldownManager().set(this, cooldown);

                if (!world.isClient)
                {
//                    double yaw = Math.toRadians(player.getHeadYaw());
//                    double pitch = Math.toRadians(player.getPitch(0.1f));

                    double mult = 1.5; // Multiplier for bullet speed.
                    double vx = mult * -Math.sin(yaw) * Math.cos(pitch) + player.getVelocity().getX();
                    double vy = mult * -Math.sin(pitch) + player.getVelocity().getY();
                    double vz = mult * Math.cos(yaw) * Math.cos(pitch) + player.getVelocity().getZ();

                    Vec3d pos = new Vec3d(player.getX(), player.getY() + 1.4, player.getZ());
                    if (!player.isSneaking())
                    {
                        Vec3d transform = getMuzzleOffset(player, stack).rotateY((float) -yaw);
                        pos = pos.add(transform);
                    }

                    ExplodingShellEntity shell = new ExplodingShellEntity(world, 2, true, pos.x, pos.y, pos.z, vx, vy, vz);
                    shell.setOwner(player);
                    world.spawnEntity(shell);

                    playSound(world, player, GunSounds.FIRE_PRIMARY);

                    if (!player.isCreative()) stack.setDamage(stack.getDamage() + 1);

                    // TODO
//                    final int anim = GeckoLibUtil.guaranteeIDForStack(stack, (ServerWorld) world);
//                    GeckoLibNetwork.syncAnimation(player, this, anim, ANIM_FIRE);
//                    for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
//                    {
//                        GeckoLibNetwork.syncAnimation(otherPlayer, this, anim, ANIM_FIRE);
//                    }
                }
            }
            else // Weapon is out of ammunition.
            {
                if (!world.isClient)
                {
                    this.reload(player, stack, null);
                }
            }
        }
    }

//    @Override
//    public void onAnimationSync(int id, int state)
//    {
//        if (state == ANIM_FIRE)
//        {
//            final AnimationController<?> controller = GeckoLibUtil.getControllerForID(this.factory, id, controllerName);
//                controller.markNeedsReload();
//                controller.setAnimation(new AnimationBuilder().addAnimation("animation.heavy_cannon.fire"));
//        }
//        else if (state == ANIM_RELOAD)
//        {
//            final AnimationController<?> controller = GeckoLibUtil.getControllerForID(this.factory, id, controllerName);
//            controller.markNeedsReload();
//            controller.setAnimation(new AnimationBuilder().addAnimation("animation.heavy_cannon.reload"));
//        }
//    }
}
