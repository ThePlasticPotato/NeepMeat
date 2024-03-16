package com.neep.meatweapons.screen;

import com.neep.meatlib.api.network.ChannelFormat;
import com.neep.meatlib.api.network.ParamCodec;
import com.neep.meatlib.network.Receiver;
import com.neep.meatlib.network.ServerChannelReceiver;
import com.neep.meatweapons.MeatWeapons;
import com.neep.meatweapons.init.MWComponents;
import com.neep.meatweapons.init.MWScreenHandlers;
import com.neep.meatweapons.item.MeatgunModuleItem;
import com.neep.meatweapons.item.meatgun.MeatgunComponent;
import com.neep.meatweapons.item.meatgun.MeatgunModule;
import com.neep.meatweapons.item.meatgun.ModuleSlot;
import com.neep.neepmeat.screen_handler.BasicScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class TinkerTableScreenHandler extends BasicScreenHandler
{
    public static final int BACKGROUND_WIDTH = 340;
    public static final int BACKGROUND_HEIGHT = 200;

    public static final Identifier CHANNEL_ID = new Identifier(MeatWeapons.NAMESPACE, "chunnel");
    public static final ChannelFormat<SlotClick> CHANNEL_FORMAT = ChannelFormat.builder(SlotClick.class)
            .param(ParamCodec.UUID)
            .param(ParamCodec.INT)
            .build();

    private Receiver<SlotClick> receiver = Receiver.empty();

    public TinkerTableScreenHandler(int syncId, PlayerInventory playerInventory)
    {
        this(syncId, playerInventory, new SimpleInventory(1));
    }

    public TinkerTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory blockInv)
    {
        super(MWScreenHandlers.MEATGUN, playerInventory, blockInv, syncId, null);

        addSlot(new Slot(blockInv, 0, 4, BACKGROUND_HEIGHT - 2 - 17));
        createInventory(5 + 18, BACKGROUND_HEIGHT - 74, playerInventory);
        createHotbar(5 + 18, BACKGROUND_HEIGHT - 19, playerInventory);

        if (playerInventory.player instanceof ServerPlayerEntity serverPlayerEntity)
            this.receiver = new ServerChannelReceiver<>(serverPlayerEntity, CHANNEL_ID, CHANNEL_FORMAT, this::onSlotClick);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player)
    {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player)
    {
        super.onClosed(player);
        receiver.close();
    }

    public void onSlotClick(UUID uuid, int slotIdx)
    {
        MeatgunComponent meatgun = MWComponents.MEATGUN.getNullable(getSlot(0).getStack());
        if (meatgun != null)
        {
            MeatgunModule parent = meatgun.find(uuid);
            if (parent != null)
            {
                ModuleSlot slot1 = parent.getChildren().get(slotIdx);

                boolean slotEmpty = slot1.get() == MeatgunModule.DEFAULT;
                boolean canChange = true;
                if (!slotEmpty)
                {
                    for (var childSlot : slot1.get().getChildren())
                    {
                        if (childSlot.get() != MeatgunModule.DEFAULT)
                        {
                            canChange = false;
                        }
                    }
                }

                boolean cursorEmpty = getCursorStack().isEmpty();

                if (!slotEmpty && canChange)
                {
                    if (cursorEmpty)
                    {
                        ItemStack moduleStack = MeatgunModuleItem.get(slot1.get().getType());

                        if (moduleStack.isEmpty())
                            return;

                        setCursorStack(moduleStack);
                        slot1.set(MeatgunModule.DEFAULT);
                        syncState();
                    }
                    else if (getCursorStack().getCount() == 1)
                    {
                        // Swap
                        MeatgunModule.Type<?> cursorType = MeatgunModuleItem.get(getCursorStack());
                        if (cursorType != MeatgunModule.DEFAULT_TYPE)
                        {
                            setCursorStack(MeatgunModuleItem.get(slot1.get().getType()));

                            slot1.set(cursorType.create(meatgun.getListener(), parent));
                            syncState();
                        }
                    }
                }
                else if (slotEmpty && canChange)
                {
                    MeatgunModule.Type<?> cursorType = MeatgunModuleItem.get(getCursorStack());
                    if (cursorType != MeatgunModule.DEFAULT_TYPE)
                    {
                        slot1.set(cursorType.create(meatgun.getListener(), parent));
                        getCursorStack().decrement(1);
                        syncState();
                    }
                }
            }
        }
    }

    public interface SlotClick
    {
        void apply(UUID uuid, int slot);
    }
}
