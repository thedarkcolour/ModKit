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

package thedarkcolour.modkit.data.recipe;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.MKRecipeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NbtShapedRecipeBuilder extends NbtResultRecipe<NbtShapedRecipeBuilder> {
    private final List<String> rows = new ArrayList<>();
    private final Char2ObjectMap<Ingredient> key = new Char2ObjectOpenHashMap<>(9);
    private boolean showNotification = true;

    public NbtShapedRecipeBuilder(RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt) {
        super(category, result.asItem(), resultCount, resultNbt);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return shaped(category, result, 1);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return shaped(category, result, count, null);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count, @Nullable CompoundTag nbt) {
        return new NbtShapedRecipeBuilder(category, result, count, nbt);
    }

    public NbtShapedRecipeBuilder define(char symbol, ItemLike item) {
        return define(symbol, Ingredient.of(item));
    }

    public NbtShapedRecipeBuilder define(char symbol, TagKey<Item> tag) {
        return define(symbol, Ingredient.of(tag));
    }

    public NbtShapedRecipeBuilder define(char symbol, Ingredient ingredient) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        } else if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(symbol, ingredient);
            return this;
        }
    }

    public NbtShapedRecipeBuilder pattern(String pattern) {
        if (!this.rows.isEmpty() && pattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(pattern);
            return this;
        }
    }

    /**
     * Whether this recipe displays a toast on the top right of the player's screen upon unlocking.
     * The only Vanilla recipe where this is ever set to false is the Crafting Table recipe, which
     * is also the only recipe the player has unlocked by default.
     */
    public NbtShapedRecipeBuilder showNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    /**
     * Using the given ingredients, attempts to generate a recipe criterion instead of requiring each
     * recipe to have {@link #unlockedBy} with tons of extra information.
     */
    public void attemptAutoCriterion() {
        for (Ingredient ingredient : this.key.values()) {
            if (MKRecipeProvider.unlockedByHaving(this, ingredient)) {
                return;
            }
        }
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        ensureValid(id);
        this.advancement.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
        ShapedRecipe recipe = new ShapedRecipe(
                Objects.requireNonNullElse(this.group, ""),
                RecipeBuilder.determineBookCategory(this.category),
                ShapedRecipePattern.of(this.key, this.rows),
                new ItemStack(this.result, this.resultCount, this.resultNbt),
                this.showNotification
        );
        output.accept(id, recipe, this.advancement.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }
}
