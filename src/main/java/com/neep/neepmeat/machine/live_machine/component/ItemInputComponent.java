package com.neep.neepmeat.machine.live_machine.component;

import com.neep.neepmeat.api.live_machine.LivingMachineComponent;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.util.math.Direction;

public interface ItemInputComponent extends LivingMachineComponent
{
    Storage<ItemVariant> getStorage(Direction ctx);
}
