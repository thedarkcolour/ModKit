package thedarkcolour.testmod.data;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import thedarkcolour.modkit.impl.MKTagsProvider;
import thedarkcolour.testmod.TestMod;

public class ModTags {
    public static void addBlockTags(MKTagsProvider<Block> tags) {
        tags.tag(BlockTags.IMPERMEABLE).add(TestMod.ORANGE_BLOCK);
        tags.tag(BlockTags.WARPED_STEMS).add(TestMod.RED_BLOCK);
        // should print a warning and do nothing
        tags.copy(BlockTags.WARPED_STEMS, ItemTags.WARPED_STEMS);
    }

    public static void addItemTags(MKTagsProvider<Item> tags) {
        tags.copy(BlockTags.WARPED_STEMS, ItemTags.WARPED_STEMS);
        tags.tag(ItemTags.FOX_FOOD).add(TestMod.ORANGE);
    }
}
