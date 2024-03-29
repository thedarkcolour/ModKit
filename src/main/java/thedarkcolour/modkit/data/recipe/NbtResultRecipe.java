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

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class NbtResultRecipe<T extends RecipeBuilder> implements RecipeBuilder {
    protected final RecipeCategory category;
    protected final Item result;
    protected final int resultCount;
    @Nullable
    protected final CompoundTag resultNbt;
    protected final Advancement.Builder advancement = Advancement.Builder.advancement();
    @Nullable
    protected String group;

    public NbtResultRecipe(RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt) {
        this.category = category;
        this.result = result;
        this.resultCount = resultCount;
        this.resultNbt = resultNbt;
    }

    @Override
    public T unlockedBy(String name, CriterionTriggerInstance trigger) {
        this.advancement.addCriterion(name, trigger);
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
        return advancement.getCriteria().isEmpty();
    }

    public static String getCategoryName(RecipeCategory category) {
        return switch (category) {
            case BUILDING_BLOCKS -> "building";
            case TOOLS, COMBAT -> "equipment";
            case REDSTONE -> "redstone";
            default -> "misc";
        };
    }

    public static JsonObject serializeResult(Item item, int count, @Nullable CompoundTag nbt) {
        JsonObject resultObj = new JsonObject();
        resultObj.addProperty("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString());

        if (count > 1) {
            resultObj.addProperty("count", count);
        }
        if (nbt != null) {
            resultObj.addProperty("nbt", nbt.getAsString());
        }

        return resultObj;
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }
}
