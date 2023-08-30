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
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.NbtShapedRecipeBuilder;
import thedarkcolour.modkit.data.recipe.NbtShapelessRecipeBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.data.recipes.SmithingTransformRecipeBuilder.smithing;

/**
 * ModKit's implementation of RecipeProvider, along with some static utility methods which can be found at the bottom of this file.
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

    public void conditional(String recipeId, List<ICondition> conditions, Consumer<Consumer<FinishedRecipe>> addRecipes) {
        conditional(new ResourceLocation(this.modid, recipeId), conditions, addRecipes);
    }

    /**
     * Allows creation of conditional recipes.
     * @param recipeId The ID of the conditional recipe
     * @param conditions The list of conditions used for all recipe(s) added in addRecipes
     * @param addRecipes Add recipe(s) to the conditional recipe. Make sure you are using the Consumer from this lambda!
     */
    public void conditional(ResourceLocation recipeId, List<ICondition> conditions, Consumer<Consumer<FinishedRecipe>> addRecipes) {
        Preconditions.checkNotNull(this.writer);
        Preconditions.checkArgument(!conditions.isEmpty(), "Cannot add a recipe with no conditions.");

        var realWriter = this.writer;
        var builder = ConditionalRecipe.builder();

        try {
            this.writer = recipe -> {
                for (var condition : conditions) {
                    builder.addCondition(condition);
                }
                builder.addRecipe(recipe);
            };
            addRecipes.accept(this.writer);
        } finally {
            this.writer = realWriter;
        }

        builder.build(this.writer, recipeId);
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

        ResourceLocation id = createRecipeId(recipeId, builder.getResult());

        builder.save(this.writer, id);
    }

    public ResourceLocation defaultRecipeId(ItemLike result) {
        return createRecipeId(null, result);
    }

    /**
     * Returns a recipe ID for the given string or returns a default recipe ID based on the result item. Unlike Vanilla
     * data generation, your {@link #modid} is the default namespace, avoiding accidental recipe conflicts.
     *
     * @param recipeId A (nullable) recipe ID to convert. Default namespace is {@link #modid}, NOT "minecraft"
     * @param result   The resulting item of the crafting recipe, used only when recipeId is null.
     * @return An ID to use for a newly generated recipe
     */
    public ResourceLocation createRecipeId(@Nullable String recipeId, ItemLike result) {
        if (recipeId != null) {
            if (recipeId.contains(":")) {
                return new ResourceLocation(recipeId);
            } else {
                return new ResourceLocation(this.modid, recipeId);
            }
        } else {
            return new ResourceLocation(this.modid, MKRecipeProvider.path(result));
        }
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
    @SuppressWarnings({"unchecked", "rawtypes"})
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
                        MKRecipeProvider.unlockedByHaving(shapeless, itemLike);
                        noCriterion = false;
                    }
                } else if (ingredient instanceof TagKey tagKey) {
                    shapeless.requires(tagKey);

                    if (noCriterion) {
                        MKRecipeProvider.unlockedByHaving(shapeless, tagKey);
                        noCriterion = false;
                    }
                } else if (ingredient instanceof Ingredient ing) {
                    shapeless.requires(ing);

                    if (noCriterion) {
                        noCriterion = !MKRecipeProvider.unlockedByHaving(shapeless, ing);
                    }
                } else {
                    throw MKRecipeProvider.nonIngredientArgument(ingredient);
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
        Preconditions.checkNotNull(this.writer);

        grid3x3(RecipeCategory.BUILDING_BLOCKS, storage, Ingredient.of(material));

        ShapelessRecipeBuilder fromStorage = new ShapelessRecipeBuilder(RecipeCategory.MISC, material, 9);
        unlockedByHaving(fromStorage, storage);
        fromStorage.requires(storage);
        fromStorage.save(this.writer, id(material).withSuffix("_from_" + id(storage).getPath()));
    }

    public void grid3x3(ItemLike result, Ingredient ingredient) {
        this.grid3x3(RecipeCategory.MISC, result, ingredient);
    }

    public void grid3x3(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        Preconditions.checkNotNull(this.writer);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("###");
            recipe.pattern("###");
            recipe.pattern("###");
        });
    }

    public void grid2x2(ItemLike result, Ingredient ingredient) {
        this.grid2x2(RecipeCategory.MISC, result, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        Preconditions.checkNotNull(this.writer);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("##");
            recipe.pattern("##");
        });
    }

    public void foodCooking(ItemLike input, ItemLike result, float experience) {
        foodCooking(Ingredient.of(input), result, experience);
    }

    public void foodCooking(Ingredient ingredient, ItemLike result, float experience) {
        foodCooking(ingredient, result, experience, 200);
    }

    /**
     * Adds a furnace recipe, smoker recipe, and a campfire recipe for the given ingredient and result.
     * Useful for cooking raw foods into their cooked forms.
     *
     * @param ingredient The input ingredient, ex. Raw Beef
     * @param result     The resulting item, ex. Cooked Beef
     * @param experience The amount of experience points awarded for cooking in the furnace or smoker
     * @param duration   The time to smelt in a regular furnace. Smoker takes 0.5x as long, campfire takes 3x as long.
     */
    public void foodCooking(Ingredient ingredient, ItemLike result, float experience, int duration) {
        smelting(ingredient, result, experience, duration);
        smoking(ingredient, result, experience, duration / 2);
        campfire(ingredient, result, experience, duration * 3);
    }

    public void smelting(ItemLike input, ItemLike result, float experience) {
        smelting(Ingredient.of(input), result, experience);
    }

    public void smelting(Ingredient ingredient, ItemLike result, float experience) {
        smelting(ingredient, result, experience, 200);
    }

    public void smelting(Ingredient ingredient, ItemLike result, float experience, int duration) {
        genericCooking(RecipeSerializer.SMELTING_RECIPE, ingredient, result, experience, duration);
    }

    public void blasting(ItemLike input, ItemLike result, float experience) {
        blasting(Ingredient.of(input), result, experience);
    }

    public void blasting(Ingredient ingredient, ItemLike result, float experience) {
        blasting(ingredient, result, experience, 100);
    }

    public void blasting(Ingredient ingredient, ItemLike result, float experience, int duration) {
        genericCooking(RecipeSerializer.BLASTING_RECIPE, ingredient, result, experience, duration);
    }

    public void smoking(ItemLike input, ItemLike result, float experience) {
        smoking(Ingredient.of(input), result, experience);
    }

    public void smoking(Ingredient ingredient, ItemLike result, float experience) {
        smoking(ingredient, result, experience, 100);
    }

    public void smoking(Ingredient ingredient, ItemLike result, float experience, int duration) {
        genericCooking(RecipeSerializer.SMOKING_RECIPE, ingredient, result, experience, duration);
    }

    public void campfire(ItemLike input, ItemLike result, float experience) {
        campfire(Ingredient.of(input), result, experience, 600);
    }

    public void campfire(Ingredient ingredient, ItemLike result, float experience) {
        campfire(ingredient, result, experience, 600);
    }

    public void campfire(Ingredient ingredient, ItemLike result, float experience, int duration) {
        genericCooking(RecipeSerializer.CAMPFIRE_COOKING_RECIPE, ingredient, result, experience, duration);
    }

    public void genericCooking(RecipeSerializer<? extends AbstractCookingRecipe> serializer, Ingredient ingredient, ItemLike result, float experience, int duration) {
        genericCooking(RecipeCategory.MISC, serializer, ingredient, result, experience, duration);
    }

    public void genericCooking(RecipeCategory category, RecipeSerializer<? extends AbstractCookingRecipe> serializer, Ingredient ingredient, ItemLike result, float experience, int duration) {
        Preconditions.checkNotNull(this.writer);

        var builder = SimpleCookingRecipeBuilder.generic(ingredient, category, result, experience, duration, serializer);
        unlockedByHaving(builder, ingredient);
        String id = path(result);
        if (serializer == RecipeSerializer.CAMPFIRE_COOKING_RECIPE) {
            id += "_from_campfire_cooking";
        } else if (serializer == RecipeSerializer.BLASTING_RECIPE) {
            id += "_from_blasting";
        } else if (serializer == RecipeSerializer.SMOKING_RECIPE) {
            id += "_from_smoking";
        }
        builder.save(this.writer, createRecipeId(id, result.asItem()));
    }

    public void netheriteUpgrade(RecipeCategory category, Ingredient input, ItemLike result) {
        Preconditions.checkNotNull(this.writer);

        unlockedByHaving(smithing(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), input, Ingredient.of(Tags.Items.INGOTS_NETHERITE), category, result.asItem()), Tags.Items.INGOTS_NETHERITE).save(this.writer, defaultRecipeId(result));
    }

    /**
     * @return The registry name/ID of the given item
     */
    @SuppressWarnings("deprecation")
    public static ResourceLocation id(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key().location();
    }

    /**
     * Takes in an Ingredient and tries to extract its Item or TagKey for making a recipe criterion.
     * This is necessary because an Ingredient cannot be used for normal recipe criterion.
     *
     * @param builder    The recipe builder to add a criterion to (can be RecipeBuilder or the smithing recipe builders)
     * @param ingredient The ingredient to try to use as a criterion
     * @return True if a criterion was added to the recipe
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
                    MKRecipeProvider.unlockedByHaving(builder, tag);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sets a recipe's unlockedBy criterion to InventoryChangeTrigger.TriggerInstance.has(TagKey),
     * which is protected and thus normally restricted to subclasses of RecipeProvider.
     *
     * @param recipeBuilder The recipe builder
     * @param item          The tag the player must have in their inventory to unlock the recipe
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
     * @return The recipe builder
     */
    public static <T> T unlockedByHaving(T recipeBuilder, TagKey<Item> tag) {
        return unlockedBy(recipeBuilder, has(tag));
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

    public static String path(ItemLike item) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.asItem()), "Item " + item.asItem() + " not found in items registry!").getPath();
    }

    public static Ingredient ingredient(ItemLike item) {
        return Ingredient.of(item);
    }

    public static Ingredient ingredient(ItemLike... items) {
        return Ingredient.of(items);
    }

    public static Ingredient ingredient(Supplier<? extends ItemLike> item) {
        return ingredient(item.get());
    }

    @SafeVarargs
    public static Ingredient ingredient(Supplier<? extends ItemLike>... items) {
        ItemLike[] values = new ItemLike[items.length];
        for (int i = 0; i < items.length; i++) {
            values[i] = items[i].get();
        }
        return ingredient(values);
    }

    public static Ingredient ingredient(TagKey<Item> tag) {
        return Ingredient.of(tag);
    }

    @SafeVarargs
    public static Ingredient ingredient(TagKey<Item>... tags) {
        return Ingredient.fromValues(Arrays.stream(tags).map(Ingredient.TagValue::new));
    }

    /**
     * Creates an ingredient with a series of values, a combination of the following types:
     * <ul>
     *     <li>{@link ItemLike}</li>
     *     <li>{@link TagKey}&lt;{@link Item}&gt;</li>
     *     <li>{@link Supplier}&lt;? extends {@link ItemLike}&gt;</li>
     *     <li>{@link Ingredient.Value}</li>
     * </ul>
     * @param values The values the ingredient can match against
     * @return An ingredient with the specified values
     * @throws IllegalArgumentException If any element in {@code values} is not one of the permitted types listed above
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Ingredient ingredient(Object... values) {
        return Ingredient.fromValues(Arrays.stream(values).map(value -> {
            if (value instanceof Ingredient.Value ingredientValue) {
                return ingredientValue;
            } else if (value instanceof TagKey itemTag && itemTag.registry().equals(Registries.ITEM)) {
                return new Ingredient.TagValue(itemTag);
            } else if (value instanceof ItemLike itemLike) {
                return new Ingredient.ItemValue(new ItemStack(itemLike));
            } else if (value instanceof Supplier<?> supplier && supplier.get() instanceof ItemLike itemLike) {
                return new Ingredient.ItemValue(new ItemStack(itemLike));
            } else {
                throw new IllegalArgumentException("Invalid Ingredient value: " + value.getClass() + " is not subclass of Ingredient.Value, TagKey<Item>, ItemLike, or Supplier<? extends ItemLike>");
            }
        }));
    }

    private static IllegalArgumentException nonIngredientArgument(Object item) {
        return new IllegalArgumentException("Argument " + item + " is not instance of Ingredient, TagKey, or ItemLike");
    }
}
