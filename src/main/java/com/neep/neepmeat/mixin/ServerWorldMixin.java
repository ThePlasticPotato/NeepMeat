package com.neep.neepmeat.mixin;

import com.neep.meatlib.util.LazySupplier;
import com.neep.neepmeat.enlightenment.EnlightenmentEventManager;
import com.neep.neepmeat.transport.blood_network.BloodNetworkManager;
import com.neep.neepmeat.transport.event.WorldChunkEvents;
import com.neep.neepmeat.transport.fluid_network.FluidNodeManager;
import com.neep.neepmeat.transport.fluid_network.FluidNodeManagerImpl;
import com.neep.neepmeat.transport.interfaces.IServerWorld;
import com.neep.neepmeat.transport.item_network.PipeCacheImpl;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements IServerWorld
{
    @Shadow public abstract void removePlayer(ServerPlayerEntity player, Entity.RemovalReason reason);

    @Shadow public abstract PersistentStateManager getPersistentStateManager();

    @Unique public FluidNodeManager neepmeat$nodeManager = new FluidNodeManagerImpl((ServerWorld) (Object) this);
    @Unique public PipeCacheImpl neepmeat$itemNetwork = new PipeCacheImpl((ServerWorld) (Object) this);
    @Unique public EnlightenmentEventManager neepmeat$enlightenmentEventManager = new EnlightenmentEventManager();

//    @Unique private BloodNetworkManager neepmeat$bloodNetworkManager = new BloodNetworkManager((ServerWorld) (Object) this);
    @Unique private LazySupplier<BloodNetworkManager> neepmeat$bloodNetworkManager = LazySupplier.of(() ->
        getPersistentStateManager().getOrCreate(nbt -> new BloodNetworkManager((ServerWorld) (Object) this, nbt),
                () -> new BloodNetworkManager((ServerWorld) (Object) this),
                BloodNetworkManager.NAME));

    @Inject(method = "unloadEntities", at = @At(value = "HEAD"))
    public void onUnloadEntities(WorldChunk chunk, CallbackInfo ci)
    {
        WorldChunkEvents.UNLOAD_ENTITIES.invoker().load(chunk);
    }

    @Override
    public FluidNodeManager getFluidNodeManager()
    {
        return neepmeat$nodeManager;
    }

    @Override
    public PipeCacheImpl getItemNetwork()
    {
        return neepmeat$itemNetwork;
    }

    @Override
    public EnlightenmentEventManager getEnlightenmentEventManager()
    {
        return neepmeat$enlightenmentEventManager;
    }

    @Override
    public BloodNetworkManager getBloodNetworkManager() { return neepmeat$bloodNetworkManager.get(); }
}
