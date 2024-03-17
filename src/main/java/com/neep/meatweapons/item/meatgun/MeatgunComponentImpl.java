package com.neep.meatweapons.item.meatgun;

import com.neep.meatweapons.network.MWAttackC2SPacket;
import com.neep.meatweapons.network.MeatgunModuleNetwork;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.item.ItemComponent;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MeatgunComponentImpl extends ItemComponent implements MeatgunComponent
{
    private final RecoilManager recoil = new RecoilManager();
    private final BaseModule root;
    private boolean dirty = true;
    private boolean invalidated = false;
    private final Listener listener = new Listener();

    // TODO: cache modules in UUID-object map

    public MeatgunComponentImpl(ItemStack stack, ComponentKey<MeatgunComponent> key)
    {
        super(stack, key);
        root = new BaseModule(listener);
        root.readNbt(getOrCreateRootTag());
    }

    @Override
    public MeatgunModule getRoot()
    {
        return root;
    }

    @Override
    public UUID getUuid()
    {
        UUID foundUUID = getUuid("meatgun_uuid");
        if (foundUUID == null)
        {
            putUuid("meatgun_uuid", UUID.randomUUID());
            return getUuid("meatgun_uuid");
        }
        return foundUUID;
    }

    public void trigger(World world, PlayerEntity player, ItemStack stack, int id, double pitch, double yaw, MWAttackC2SPacket.HandType handType)
    {
        root.trigger(world, player, stack, id, pitch, yaw, handType);
//        var module = root.getChildren().get(0).get();
//        if (module instanceof BosherModule)
//        {
//            root.getChildren().get(0).set(new UnderbarrelModule());
//            markDirty();
//        }
//        else
//        {
//            root.getChildren().get(0).set(new BosherModule());
//            markDirty();
//        }
    }

    @Override
    public void tickTrigger(World world, PlayerEntity player, ItemStack stack, int id, double pitch, double yaw, MWAttackC2SPacket.HandType handType)
    {
        root.tickTrigger(world, player, stack, id, pitch, yaw, handType);
    }

    @Override
    public RecoilManager getRecoil()
    {
        return recoil;
    }

    @Override
    public void tick(PlayerEntity player)
    {
        root.tick(player);

        if (dirty)
        {
            root.writeNbt(getOrCreateRootTag());
            dirty = false;
        }

        if (invalidated)
        {
            root.readNbt(getOrCreateRootTag());
            invalidated = false;
        }
    }

    @Override
    public void markDirty()
    {
        if (root != null)
            root.writeNbt(getOrCreateRootTag());
    }

    @Override
    @Nullable
    public MeatgunModule find(UUID uuid)
    {
        return findRecursive(root, uuid);
    }

    @Override
    public Listener getListener()
    {
        return listener;
    }

    @Nullable
    private MeatgunModule findRecursive(MeatgunModule module, UUID uuid)
    {
        if (module.getUuid().equals(uuid))
            return module;

        for (var slot : module.getChildren())
        {
            MeatgunModule child = slot.get();
            if (child == MeatgunModule.DEFAULT)
                continue;

            if (child.getUuid().equals(uuid))
                return child;

            return findRecursive(child, uuid);
        }
        return null;
    }

    @Override
    public void onTagInvalidated()
    {
        super.onTagInvalidated();
        dirty = true;
        invalidated = true;
    }

    public int getInt()
    {
        return getInt("ooer");
    }

    public void writeInt(int i)
    {
        putInt("ooer", i);
    }

    private class Listener implements MeatgunComponent.Listener
    {
        @Override
        public MeatgunComponent get()
        {
            return MeatgunComponentImpl.this;
        }

        @Override
        public PacketByteBuf getBuf(MeatgunModule module)
        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(get().getUuid());
            buf.writeUuid(module.getUuid());
            return buf;
        }

        @Override
        public void send(PlayerEntity player, PacketByteBuf buf)
        {
            if (player instanceof ServerPlayerEntity serverPlayerEntity)
                MeatgunModuleNetwork.send(serverPlayerEntity, buf);
        }

        @Override
        public void markDirty()
        {
            MeatgunComponentImpl.this.markDirty();
        }
    }
}
