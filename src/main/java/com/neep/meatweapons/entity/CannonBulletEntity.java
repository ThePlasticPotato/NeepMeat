package com.neep.meatweapons.entity;

import com.neep.meatweapons.MeatWeapons;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

public class CannonBulletEntity extends PersistentProjectileEntity
{

    public CannonBulletEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world)
    {
        super(entityType, world);
    }

    public CannonBulletEntity(World world, double x, double y, double z, double vx, double vy, double vz)
    {
        super(MeatWeapons.CANNON_BULLET, x, y, z, world);
        this.setVelocity(vx, vy, vz);
        this.setNoGravity(false);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket()
    {
        return super.createSpawnPacket();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (getWorld().isClient)
        {
               this.spawnParticles(2);
        }
        else if (this.inGround || this.distanceTraveled > 30 || this.getVelocity().length() < 1)
        {
            getWorld().sendEntityStatus(this, (byte)0);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    private void spawnParticles(int amount)
    {
        getWorld().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
    }

    protected void onHit(LivingEntity target)
    {
        super.onHit(target);
    }

    protected ItemStack asItemStack()
    {
            return new ItemStack(Items.ARROW);
    }
}
