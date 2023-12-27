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

import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Same as regular ShapelessRecipeBuilder but you can add an NBT tag to the crafting result,
 * which is supported by Forge but is not included in Minecraft's data generation.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NbtShapelessRecipeBuilder extends NbtResultRecipe<NbtShapelessRecipeBuilder> {
    private final NonNullList<Ingredient> ingredients = NonNullList.create();

    public NbtShapelessRecipeBuilder(RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt) {
        super(category, result, resultCount, resultNbt);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), 1, null);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, CompoundTag tag) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), 1, tag);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int resultCount) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), resultCount, null);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int resultCount, CompoundTag tag) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), resultCount, tag);
    }

    public NbtShapelessRecipeBuilder requires(TagKey<Item> tag) {
        return requires(Ingredient.of(tag));
    }

    public NbtShapelessRecipeBuilder requires(ItemLike tag) {
        return requires(Ingredient.of(tag));
    }

    public NbtShapelessRecipeBuilder requires(ItemLike tag, int quantity) {
        return requires(Ingredient.of(tag), quantity);
    }

    public NbtShapelessRecipeBuilder requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }

    public NbtShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
        for (int i = 0; i < quantity; i++) {
            this.ingredients.add(ingredient);
        }

        return this;
    }

    @Override
    public void save(RecipeOutput writer, ResourceLocation id) {
        ensureValid(id);
        this.advancement.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
        ShapelessRecipe recipe = new ShapelessRecipe(
                Objects.requireNonNullElse(this.group, ""),
                RecipeBuilder.determineBookCategory(this.category),
                new ItemStack(this.result, this.resultCount, this.resultNbt),
                this.ingredients
        );
        writer.accept(id, recipe, this.advancement.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }
}
