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

package thedarkcolour.modkit.data;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.NbtShapedRecipeBuilder;
import thedarkcolour.modkit.data.recipe.NbtShapelessRecipeBuilder;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The static methods of this class are useful helper methods and can be found at the bottom of this file.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class MKRecipeProvider extends RecipeProvider {
    private final String modid;
    private final BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes;
    @Nullable
    private Consumer<FinishedRecipe> writer;

    protected MKRecipeProvider(PackOutput output, String modid, BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes) {
        super(output);
        this.modid = modid;
        this.addRecipes = addRecipes;
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        this.writer = writer;
        this.addRecipes.accept(writer, this);
        this.writer = null;
    }

    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, 1, recipe);
    }

    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, int resultCount, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, resultCount, null, recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, 1, recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, resultCount, null, recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(null, category, result, resultCount, resultNbt, recipe);
    }

    /**
     * Generates a shaped recipe with the recipe layout defined by the {@code recipe} Consumer.
     * Will make a best-guess attempt for an unlockedBy criterion, but manually setting one
     * in the Consumer may be preferable or required.
     *
     * @param recipeId    Recipe id to use when generating the recipe, or null for the default name.
     * @param category    Recipe category for displaying in the green recipe book
     * @param result      The result item
     * @param resultCount The number of result items resulting from one craft of this recipe
     * @param resultNbt   The NBT of the result item(s)
     * @param recipe      Function, usually a lambda, which defines the recipe layout by calling define and key on the recipe builder.
     */
    public void shapedCrafting(@Nullable String recipeId, RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt, Consumer<NbtShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(this.writer);

        NbtShapedRecipeBuilder builder = new NbtShapedRecipeBuilder(category, result, resultCount, resultNbt);
        recipe.accept(builder);
        if (builder.isMissingCriterion()) {
            builder.attemptAutoCriterion();
        }

        ResourceLocation id;
        if (recipeId != null) {
            if (recipeId.contains(":")) {
                id = new ResourceLocation(recipeId);
            } else {
                id = new ResourceLocation(this.modid, recipeId);
            }
        } else {
            id = new ResourceLocation(this.modid, Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(builder.getResult())).getPath());
        }
        builder.save(this.writer, id);
    }

    public void shapelessCrafting(RecipeCategory category, ItemLike result, int resultCount, Object... ingredients) {
        shapelessCrafting(category, new ItemStack(result, resultCount, null), ingredients);
    }


    public void shapelessCrafting(RecipeCategory category, ItemStack result, Object... ingredients) {
        shapelessCrafting(category, result, null, ingredients);
    }

    /**
     * Generates a shapeless recipe with a list of ingredients (can be a mix of ItemLike, Ingredient, and/or TagKey)
     * and attempts to also generate a recipe criterion so (hopefully) you don't need to call {@code unlockedBy}
     *
     * @param category    The recipe category for showing in the green recipe book
     * @param result      The resulting item of this recipe (NBT and count are included in the generated recipe)
     * @param unlockedBy  A (nullable) pair of criterion name and criterion instance for unlocking the recipe.
     *                    In most cases it is easier to leave this null, but it may be desirable to pick a specific
     *                    criterion or required if ModKit cannot determine a criterion automatically.
     * @param ingredients Can be ItemLike, Ingredient, RegistryObject or TagKey
     * @throws IllegalArgumentException if any element of {@code ingredients} is not ItemLike, Ingredient, or TagKey
     */
    @SuppressWarnings("unchecked")
    public void shapelessCrafting(RecipeCategory category, ItemStack result, @Nullable Pair<String, CriterionTriggerInstance> unlockedBy, Object... ingredients) {
        Preconditions.checkNotNull(writer);

        NbtShapelessRecipeBuilder shapeless = new NbtShapelessRecipeBuilder(category, result.getItem(), result.getCount(), result.getTag());

        if (unlockedBy != null) {
            shapeless.unlockedBy(unlockedBy.left(), unlockedBy.right());
        } else {
            boolean noCriterion = true;

            for (Object ingredient : ingredients) {
                Preconditions.checkNotNull(ingredient);

                if (ingredient instanceof RegistryObject<?> obj) {
                    ingredient = obj.get();
                    Preconditions.checkArgument(ingredient instanceof ItemLike);
                }

                if (ingredient instanceof ItemLike itemLike) {
                    shapeless.requires(itemLike);

                    if (noCriterion) {
                        unlockedByHaving(shapeless, itemLike);
                        noCriterion = false;
                    }
                } else if (ingredient instanceof TagKey<?>) {
                    shapeless.requires((TagKey<Item>) ingredient);

                    if (noCriterion) {
                        unlockedByHaving(shapeless, (TagKey<Item>) ingredient);
                        noCriterion = false;
                    }
                } else if (ingredient instanceof Ingredient ing) {
                    shapeless.requires(ing);

                    if (noCriterion) {
                        noCriterion = unlockedByHaving(shapeless, ing);
                    }
                } else {
                    throw nonIngredientArgument(ingredient);
                }
            }

            if (noCriterion && shapeless.isMissingCriterion()) {
                throw new IllegalStateException("Argument list must contain one TagKey or ItemLike for adding automatic advancement criterion");
            }
        }

        shapeless.save(writer);
    }

    /**
     * Template for recipes which convert ingot <---> block. Also works for nugget <---> ingot.
     * Two recipes are generated by this method, but the recipe to convert from storage back
     * into material has the id "[modid]:[material]_from_storage" to avoid conflicts.
     *
     * @param storage  The result of the 3x3 recipe (iron block from ingots, iron ingot from nuggets, etc.)
     * @param material The ingredient of the 3x3 (iron ingot for block, iron nugget for ingot, diamond for block, etc.)
     */
    public void storage3x3(ItemLike storage, ItemLike material) {
        Preconditions.checkNotNull(writer);

        shapedCrafting(RecipeCategory.BUILDING_BLOCKS, storage, recipe -> {
            recipe.pattern("###");
            recipe.pattern("###");
            recipe.pattern("###");
            recipe.define('#', material);
        });

        ShapelessRecipeBuilder fromStorage = new ShapelessRecipeBuilder(RecipeCategory.MISC, material, 9);
        unlockedByHaving(fromStorage, storage);
        fromStorage.requires(storage);
        fromStorage.save(writer, id(material).withSuffix("_from_" + id(storage).getPath()));
    }

    /**
     * @return The registry name/ID of the given item
     */
    @SuppressWarnings("deprecation")
    public static ResourceLocation id(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key().location();
    }

    private static <T> T unlockedBy(T recipeBuilder, CriterionTriggerInstance criterion) {
        if (recipeBuilder instanceof RecipeBuilder b) {
            b.unlockedBy("has_item", criterion);
        } else if (recipeBuilder instanceof SmithingTrimRecipeBuilder b) {
            b.unlocks("has_item", criterion);
        } else if (recipeBuilder instanceof SmithingTransformRecipeBuilder b) {
            b.unlocks("has_item", criterion);
        } else {
            throw new IllegalArgumentException("Unknown recipe builder type: " + recipeBuilder.getClass().getName());
        }

        return recipeBuilder;
    }

    /**
     * Sets a recipe's unlockedBy criterion to InventoryChangeTrigger.TriggerInstance.has(TagKey),
     * which is protected and thus normally restricted to subclasses of RecipeProvider.
     *
     * @param recipeBuilder The recipe builder
     * @param item          The tag the player must have in their inventory to unlock the recipe
     *
     * @return The recipe builder
     */
    public static <T> T unlockedByHaving(T recipeBuilder, ItemLike item) {
        return unlockedBy(recipeBuilder, has(item));
    }

    /**
     * Sets a recipe's unlockedBy criterion to InventoryChangeTrigger.TriggerInstance.has(TagKey),
     * which is protected and thus normally restricted to subclasses of RecipeProvider.
     *
     * @param recipeBuilder The recipe builder
     * @param tag           The tag the player must have in their inventory to unlock the recipe
     *
     * @return The recipe builder
     */
    public static <T> T unlockedByHaving(T recipeBuilder, TagKey<Item> tag) {
        return unlockedBy(recipeBuilder, has(tag));
    }

    /**
     * Takes in an Ingredient and tries to extract its Item or TagKey for making a recipe criterion.
     * This is necessary because an Ingredient cannot be used for normal recipe criterion.
     *
     * @param builder    The recipe builder to add a criterion to (can be RecipeBuilder or the smithing recipe builders)
     * @param ingredient The ingredient to try to use as a criterion
     * @return Whether a criterion was added or not
     */
    public static boolean unlockedByHaving(Object builder, Ingredient ingredient) {
        if (ingredient.getItems().length == 1) {
            if (ingredient.toJson() instanceof JsonObject ingredientObj) {
                if (ingredientObj.has("item")) {
                    ItemStack stack = ingredient.getItems()[0];
                    MKRecipeProvider.unlockedByHaving(builder, stack.getItem());
                    return true;
                } else if (ingredientObj.has("tag")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(ingredientObj.get("tag").getAsString()));
                    unlockedByHaving(builder, tag);
                    return true;
                }
            }
        }

        return false;
    }

    private static IllegalArgumentException nonIngredientArgument(Object item) {
        return new IllegalArgumentException("Argument " + item + " is not instance of Ingredient, TagKey, or ItemLike");
    }
}
