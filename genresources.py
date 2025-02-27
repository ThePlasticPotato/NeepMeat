#! ~/opt/mcpython/bin/pythn

import mcresources
from mcresources import block_states
rm = mcresources.ResourceManager('neepmeat')

blocks = [
    'grey_rough_concrete',
    'yellow_rough_concrete',
    'white_rough_concrete',
    'red_rough_concrete',
    'blue_rough_concrete',
    'yellow_tiles',
    'filled_scaffold',
    'caution_block',
    'polished_metal_small_bricks',
    'dirty_red_tiles',
    'dirty_white_tiles',
    'sandy_bricks',
    'meat_steel_block',
    'duat_stone',
    'duat_cobblestone',
    'bloody_bricks',
    'bloody_tiles',
    'asbestos_tile',
    'asbestos_shingle'
]

wood_blocks = [
    # 'blood_bubble_planks',
]

doors = [
    'caution_block'
]

# bl = rm.block('asbestos')
# bl.with_blockstate()
# bl.with_block_model()
# bl.with_item_model()

for name in blocks:
    bl = rm.block(name)
    bl.with_blockstate()
    bl.with_block_model()
    bl.with_item_model()
    bl.make_slab()
    bl.make_stairs()
    bl.make_wall()

for name in wood_blocks:
    bl = rm.block(name)
    bl.with_blockstate()
    bl.with_block_model()
    bl.with_item_model()
    bl.make_slab()
    bl.make_stairs()
    bl.make_wall()
    bl.make_trapdoor()
    bl.make_door()
    bl.make_button()
    bl.make_pressure_plate()
    bl.make_fence_gate()

for name in doors:
    bl = rm.block(name)
    bl.make_door()