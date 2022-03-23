package com.neep.assembly;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DebugItem extends Item
{
    public DebugItem(Settings settings)
    {
        super(settings);
    }

    public ActionResult useOnBlock(ItemUsageContext context)
    {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Assembly.PLATFORM))
        {
            if (!world.isClient)
            {
                AssemblyUtils.assembleBlocks(world, pos);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }
}
