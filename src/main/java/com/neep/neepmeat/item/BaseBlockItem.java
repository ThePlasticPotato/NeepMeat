package com.neep.neepmeat.item;

import com.neep.neepmeat.ItemGroups;
import com.neep.neepmeat.init.ItemInit;
import com.neep.neepmeat.NeepMeat;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class BaseBlockItem extends BlockItem implements BaseItem
{
    private final String name;
    private final boolean hasLore;

    public BaseBlockItem(Block block, String itemName, int itemMaxStack, boolean hasLore)
    {
        super(block, new FabricItemSettings().maxCount(itemMaxStack).group(ItemGroups.GENERAL));
        this.name = itemName;
        this.hasLore = hasLore;
        ItemInit.ITEMS.add(this);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        if (hasLore)
        {
            tooltip.add(new TranslatableText("item." + NeepMeat.NAMESPACE + "." + name + ".lore").formatted(Formatting.GRAY));
        }
    }

    public String getItemName()
    {
        return name;
    }
}
