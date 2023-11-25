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

import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import thedarkcolour.modkit.data.MKRecipeProvider;
import thedarkcolour.testmod.TestMod;

import java.util.List;
import java.util.function.Consumer;

// Package private so that multiple mods can use these names without having a ton of autocomplete options
class Recipes {
    static void addRecipes(Consumer<FinishedRecipe> writer, MKRecipeProvider recipes) {
        recipes.storage3x3(TestMod.ORANGE_BLOCK.get(), TestMod.ORANGE.get());

        recipes.conditional("apples_if_true", List.of(new ItemExistsCondition("minecraft", "bundle")), appender -> {
            recipes.grid2x2(Items.APPLE, MKRecipeProvider.ingredient(Items.DIRT));
        });

        // a recipe with 8 cobblestone and 1 black dye to give blackstone
        recipes.shapelessCrafting(RecipeCategory.BUILDING_BLOCKS, Items.BLACKSTONE, 8, ObjectIntPair.of(Tags.Items.COBBLESTONE, 8), Items.BLACK_DYE);

        recipes.woodenDoor(Items.IRON_DOOR, Items.IRON_BLOCK);
        recipes.slab(Items.CACTUS, Items.RED_CANDLE);
        recipes.stairs(Items.ICE, Items.BLACK_BED);
    }
}
