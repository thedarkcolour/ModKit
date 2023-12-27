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

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public abstract class NbtResultRecipe<T extends RecipeBuilder> implements RecipeBuilder {
    protected final RecipeCategory category;
    protected final Item result;
    protected final int resultCount;
    @Nullable
    protected final CompoundTag resultNbt;
    @SuppressWarnings("removal")
    protected final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
    @Nullable
    protected String group;
    private boolean hasCriteria;

    public NbtResultRecipe(RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt) {
        this.category = category;
        this.result = result;
        this.resultCount = resultCount;
        this.resultNbt = resultNbt;
    }

    @Override
    public T unlockedBy(String name, Criterion<?> trigger) {
        this.advancement.addCriterion(name, trigger);
        this.hasCriteria = true;
        return self();
    }

    @Override
    public T group(@Nullable String group) {
        this.group = group;
        return self();
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    protected void ensureValid(ResourceLocation id) {
        if (isMissingCriterion()) {
            throw new IllegalStateException("Now way of obtaining recipe " + id);
        }
    }

    public boolean isMissingCriterion() {
        return !this.hasCriteria;
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }
}
