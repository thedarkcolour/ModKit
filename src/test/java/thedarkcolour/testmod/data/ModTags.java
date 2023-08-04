/*
 * MIT License
 *
 * Copyright (c) 2023 thedarkcolour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package thedarkcolour.testmod.data;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import thedarkcolour.modkit.data.MKTagsProvider;
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
