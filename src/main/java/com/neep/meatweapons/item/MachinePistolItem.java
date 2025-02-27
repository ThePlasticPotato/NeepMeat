package com.neep.meatweapons.item;

import com.neep.meatlib.item.MeatlibItemSettings;
import com.neep.meatweapons.MWItems;
import com.neep.meatweapons.client.model.PistolItemModel;
import com.neep.meatweapons.network.MWAttackC2SPacket;
import com.neep.neepmeat.init.NMSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MachinePistolItem extends BaseGunItem implements Aimable
{
    public String controllerName = "controller";

    public MachinePistolItem()
    {
        super("machine_pistol", MWItems.BALLISTIC_CARTRIDGE, 24, 10, false, new MeatlibItemSettings());
        this.sounds.put(GunSounds.FIRE_PRIMARY, NMSounds.HAND_CANNON_FIRE);
        this.sounds.put(GunSounds.RELOAD, NMSounds.HAND_CANNON_RELOAD);
    }

    @Override
    public UseAction getUseAction(ItemStack stack)
    {
        return UseAction.NONE;
    }

    @Override
    public Vector3f getAimOffset()
    {
        return new Vector3f(0.0f, 0, 0);
    }

    @Override
    public Vec3d getMuzzleOffset(LivingEntity entity, ItemStack stack)
    {
        boolean sneak = entity.isSneaking();
        return new Vec3d(
                sneak ? 0 : entity.getMainHandStack().equals(stack) ? -0.2 : 0.2,
                sneak ? -0.25 : 0.1,
                .2);
    }

    @Override
    public void trigger(World world, PlayerEntity player, ItemStack stack, int id, double pitch, double yaw, MWAttackC2SPacket.HandType handType)
    {
        if (!player.getItemCooldownManager().isCoolingDown(this))
        {
            if (getShots(stack, 0) > 0)
            {
                player.getItemCooldownManager().set(this, 1);

                if (!world.isClient)
                {
                    fireBeam(world, player, stack);
                }
            }
            else // Weapon is out of ammunition.
            {
                if (world.isClient)
                {
                    // Play empty sound.
                }
                else
                {
                    // Try to reload
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
//                controller.setAnimation(new AnimationBuilder().addAnimation("animation.machine_pistol.fire"));
//        }
//        else if (state == ANIM_RELOAD)
//        {
//            final AnimationController<?> controller = GeckoLibUtil.getControllerForID(this.factory, id, controllerName);
//            controller.markNeedsReload();
//            controller.setAnimation(new AnimationBuilder().addAnimation("animation.machine_pistol.reload_r"));
//        }
//    }

    @Override
    protected GeoModel<? extends BaseGunItem> createModel()
    {
        return new PistolItemModel();
    }
}
