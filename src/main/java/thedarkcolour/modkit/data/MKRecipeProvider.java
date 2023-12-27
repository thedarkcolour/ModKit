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
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
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
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.ConditionalRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.NbtShapedRecipeBuilder;
import thedarkcolour.modkit.data.recipe.NbtShapelessRecipeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes;
    @Nullable
    private RecipeOutput output;

    protected MKRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modid, BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes) {
        super(output, lookupProvider);
        this.modid = modid;
        this.addRecipes = addRecipes;
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        this.output = output;
        this.addRecipes.accept(output, this);
        this.output = null;
    }

    public void conditional(String recipeId, List<ICondition> conditions, Consumer<RecipeOutput> addRecipes) {
        conditional(new ResourceLocation(this.modid, recipeId), conditions, addRecipes);
    }

    /**
     * Allows creation of conditional recipes.
     *
     * @param recipeId   The ID of the conditional recipe
     * @param conditions The list of conditions used for all recipe(s) added in addRecipes
     * @param addRecipes Add recipe(s) to the conditional recipe. Make sure you are using the Consumer from this lambda!
     */
    public void conditional(ResourceLocation recipeId, List<ICondition> conditions, Consumer<RecipeOutput> addRecipes) {
        Preconditions.checkNotNull(this.output);
        Preconditions.checkArgument(!conditions.isEmpty(), "Cannot add a recipe with no conditions.");

        RecipeOutput outputWithConditions = this.output.withConditions(conditions.toArray(ICondition[]::new));

        pushRecipeOutput(outputWithConditions, addRecipes);
    }

    /**
     * This method temporarily changes the {@link RecipeOutput} used for the finished recipe writer.
     * By default, the writer used by MKRecipeProvider is provided in {@link RecipeProvider#run(CachedOutput)}.
     * This method may be useful when an alternative behavior for handling FinishedRecipes generated by this class
     * is desired. One example is with {@link ConditionalRecipe}, which provides its own recipe builder for
     * accepting multiple recipes.
     *
     * @param newOutput The finished recipe output used to handle recipes generated in this method call
     * @param action    A consumer which receives the new recipe writer for generating new recipes
     * @see #conditional(String, List, Consumer)
     */
    public void pushRecipeOutput(RecipeOutput newOutput, Consumer<RecipeOutput> action) {
        Preconditions.checkNotNull(newOutput);

        RecipeOutput realWriter = this.output;
        this.output = newOutput;

        try {
            action.accept(this.output);
        } finally {
            this.output = realWriter;
        }
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
        Preconditions.checkNotNull(this.output);

        NbtShapedRecipeBuilder builder = new NbtShapedRecipeBuilder(category, result, resultCount, resultNbt);
        recipe.accept(builder);
        if (builder.isMissingCriterion()) {
            builder.attemptAutoCriterion();
        }

        ResourceLocation id = createRecipeId(recipeId, builder.getResult());

        builder.save(this.output, id);
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
        shapelessCrafting(category, new ItemStack(result, resultCount), ingredients);
    }


    public void shapelessCrafting(RecipeCategory category, ItemStack result, Object... ingredients) {
        shapelessCrafting(category, result, null, ingredients);
    }

    /**
     * Generates a shapeless recipe with a list of ingredients (can be a mix of ItemLike, Ingredient, and/or TagKey)
     * and attempts to also generate a recipe criterion so (hopefully) you don't need to call {@code unlockedBy}.
     * <p>
     * Additionally, it is possible to use {@link ObjectIntPair} or {@link IntObjectPair} containing one of the above
     * types to specify that the ingredient should appear multiple times (specified by the integer of the pair). This
     * helps avoid repetition of the same ingredient several times in the ingredients list.
     *
     * @param category    The recipe category for showing in the green recipe book
     * @param result      The resulting item of this recipe (NBT and count are included in the generated recipe)
     * @param unlockedBy  A (nullable) pair of criterion name and criterion instance for unlocking the recipe.
     *                    In most cases it is easier to leave this null, but it may be desirable to pick a specific
     *                    criterion or required if ModKit cannot determine a criterion automatically.
     * @param ingredients Can be ItemLike, Ingredient, RegistryObject or TagKey. Can also be ObjectIntPair or IntObjectPair of one of the previous types.
     * @throws IllegalArgumentException if any element of {@code ingredients} is not ItemLike, Ingredient, or TagKey,
     *                                  or if {@code ingredients} exceeds 9 ingredients, including any expanded pairs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void shapelessCrafting(RecipeCategory category, ItemStack result, @Nullable Pair<String, Criterion<?>> unlockedBy, Object... ingredients) {
        Preconditions.checkNotNull(output);

        NbtShapelessRecipeBuilder shapeless = new NbtShapelessRecipeBuilder(category, result.getItem(), result.getCount(), result.getTag());

        if (unlockedBy != null) {
            shapeless.unlockedBy(unlockedBy.left(), unlockedBy.right());
        } else {
            boolean noCriterion = true;
            // Expand the ObjectIntPair and IntObjectPair ingredients to several references to the same ingredient
            ArrayList<Object> rawIngredients = expandPairIngredients(ingredients);

            for (Object ingredient : rawIngredients) {
                Preconditions.checkNotNull(ingredient);

                // Accounts for DeferredItem and DeferredBlock
                if (ingredient instanceof DeferredHolder<?, ?> obj) {
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

        shapeless.save(output);
    }

    // Helper method to handle ingredient-object pairs
    private static ArrayList<Object> expandPairIngredients(Object... ingredients) {
        ArrayList<Object> flattened = new ArrayList<>();

        for (Object o : ingredients) {
            // Default is to just add the ingredient once
            Object ingredient = o;
            int count = 1;

            // Handle pairs
            if (ingredient instanceof ObjectIntPair<?> pair) {
                count = pair.rightInt();
                ingredient = pair.left();
            } else if (ingredient instanceof IntObjectPair<?> pair) {
                count = pair.leftInt();
                ingredient = pair.right();
            }

            // make it clear that recursive expanding is not permitted
            if (ingredient instanceof ObjectIntPair<?> || ingredient instanceof IntObjectPair<?>) {
                throw new IllegalArgumentException("Cannot have an ingredient which is a pair of pairs");
            }

            // Add the correct ingredient the correct number of times
            for (int j = 0; j < count; j++) {
                flattened.add(ingredient);
            }
        }

        if (flattened.size() > 9) {
            throw new IllegalArgumentException("Cannot have more than 9 ingredients in a shapeless crafting recipe");
        }

        return flattened;
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
        Preconditions.checkNotNull(this.output);

        grid3x3(RecipeCategory.BUILDING_BLOCKS, storage, Ingredient.of(material));

        ShapelessRecipeBuilder fromStorage = new ShapelessRecipeBuilder(RecipeCategory.MISC, material, 9);
        unlockedByHaving(fromStorage, storage);
        fromStorage.requires(storage);
        fromStorage.save(this.output, id(material).withSuffix("_from_" + id(storage).getPath()));
    }

    public void grid3x3(ItemLike result, Ingredient ingredient) {
        this.grid3x3(RecipeCategory.MISC, result, ingredient);
    }

    public void grid3x3(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("###");
            recipe.pattern("###");
            recipe.pattern("###");
        });
    }

    /**
     * @deprecated Use the version which accepts a RecipeCategory
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.21")
    @Deprecated(forRemoval = true)
    public void grid2x2(ItemLike result, Ingredient ingredient) {
        this.grid2x2(RecipeCategory.MISC, result, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        this.grid2x2(category, result, 1, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        this.grid2x2(category, result, 1, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient) {
        this.grid2x2(category, result, resultCount, ingredient, null);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, int resultCount, ItemLike ingredient) {
        this.grid2x2(category, result, resultCount, Ingredient.of(ingredient));
    }

    public void grid2x2(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient, @Nullable String group) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("##");
            recipe.pattern("##");
            if (group != null)
                recipe.group(group);
        });
    }

    public void grid3x2(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        grid3x2(category, result, 1, Ingredient.of(ingredient));
    }

    public void grid3x2(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        grid3x2(category, result, 1, ingredient);
    }

    public void grid3x2(RecipeCategory category, ItemLike result, int resultCount, ItemLike ingredient) {
        grid3x2(category, result, resultCount, Ingredient.of(ingredient), null);
    }

    public void grid3x2(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient) {
        grid3x2(category, result, resultCount, ingredient, null);
    }

    /**
     * A recipe whose ingredients are the same in 3 wide by 2 high grid shape, like a wall or a trapdoor.
     *
     * @param category    The recipe category tab used for displaying in the green recipe book from Vanilla
     * @param result      The result item (ex. Wall, Trapdoor)
     * @param resultCount The number of the result item crafted by this recipe
     * @param ingredient  The ingredient used by every slot of this recipe
     * @param group       If specified, the group of recipes to be shown along with in the green recipe book from Vanilla
     */
    public void grid3x2(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient, @Nullable String group) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("###");
            recipe.pattern("###");
            if (group != null)
                recipe.group(group);
        });
    }

    public void grid2x3(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        grid2x3(category, result, 1, Ingredient.of(ingredient));
    }

    public void grid2x3(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        grid2x3(category, result, 1, ingredient);
    }

    public void grid2x3(RecipeCategory category, ItemLike result, int resultCount, ItemLike ingredient) {
        grid2x3(category, result, resultCount, Ingredient.of(ingredient), null);
    }

    public void grid2x3(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient) {
        grid2x3(category, result, resultCount, ingredient, null);
    }

    /**
     * A recipe whose ingredients are the same in 2 wide by 3 high grid shape, like a door.
     *
     * @param category    The recipe category tab used for displaying in the green recipe book from Vanilla
     * @param result      The result item (ex. Door)
     * @param resultCount The number of the result item crafted by this recipe
     * @param ingredient  The ingredient used by every slot of this recipe
     * @param group       If specified, the group of recipes to be shown along with in the green recipe book from Vanilla
     */
    public void grid2x3(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient, @Nullable String group) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(category, result, recipe -> {
            recipe.define('#', ingredient);
            recipe.pattern("##");
            recipe.pattern("##");
            recipe.pattern("##");
            if (group != null)
                recipe.group(group);
        });
    }

    public void woodenDoor(ItemLike result, ItemLike input) {
        woodenDoor(result, Ingredient.of(input));
    }

    public void woodenDoor(ItemLike result, Ingredient ingredient) {
        grid2x3(RecipeCategory.REDSTONE, result, 3, ingredient, "wooden_door");
    }

    public void woodenTrapdoor(ItemLike result, ItemLike input) {
        woodenTrapdoor(result, Ingredient.of(input));
    }

    public void woodenTrapdoor(ItemLike result, Ingredient ingredient) {
        grid3x2(RecipeCategory.REDSTONE, result, 2, ingredient, "wooden_trapdoor");
    }

    public void stairs(ItemLike result, ItemLike input) {
        stairs(result, input, null);
    }

    public void stairs(ItemLike result, Ingredient ingredient) {
        stairs(result, ingredient, null);
    }

    public void stairs(ItemLike result, ItemLike input, @Nullable String group) {
        stairs(result, Ingredient.of(input), group);
    }

    public void stairs(ItemLike result, Ingredient ingredient, @Nullable String group) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(RecipeCategory.BUILDING_BLOCKS, result, 4, builder -> {
            builder.define('#', ingredient);
            builder.pattern("#  ");
            builder.pattern("## ");
            builder.pattern("###");
            if (group != null) builder.group(group);
        });
    }

    public void woodenStairs(ItemLike result, ItemLike planks) {
        stairs(result, planks, "wooden_stairs");
    }

    public void slab(ItemLike result, ItemLike input) {
        slab(result, input, null);
    }

    public void slab(ItemLike result, Ingredient ingredient) {
        slab(result, ingredient, null);
    }

    public void slab(ItemLike result, ItemLike input, @Nullable String group) {
        slab(result, Ingredient.of(input), group);
    }

    public void slab(ItemLike result, Ingredient ingredient, @Nullable String group) {
        Preconditions.checkNotNull(this.output);

        shapedCrafting(RecipeCategory.BUILDING_BLOCKS, result, 6, builder -> {
            builder.define('#', ingredient);
            builder.pattern("###");
            if (group != null) builder.group(group);
        });
    }

    public void woodenSlab(ItemLike result, ItemLike planks) {
        slab(result, planks, "wooden_slab");
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

    public void oreSmelting(ItemLike input, ItemLike result, float experience) {
        oreSmelting(Ingredient.of(input), result, experience);
    }

    public void oreSmelting(Ingredient ingredient, ItemLike result, float experience) {
        oreSmelting(ingredient, result, experience, 200);
    }

    /**
     * Adds a furnace recipe and blast furnace recipe for the given input and output. Ideal for ore recipes.
     *
     * @param ingredient The input ingredient, ex. Raw Gold Ore
     * @param result     The resulting item, ex. Gold Ingot
     * @param experience The amount of experience points awarded for smelting in the furnace or blast furnace
     * @param duration   The time to smelt in a regular furnace. Blast furnace takes 0.5x as long.
     */
    public void oreSmelting(Ingredient ingredient, ItemLike result, float experience, int duration) {
        smelting(ingredient, result, experience, duration);
        blasting(ingredient, result, experience, duration / 2);
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
        Preconditions.checkNotNull(this.output);

        String id = path(result);
        AbstractCookingRecipe.Factory<? extends AbstractCookingRecipe> factory = SmeltingRecipe::new;
        if (serializer == RecipeSerializer.CAMPFIRE_COOKING_RECIPE) {
            id += "_from_campfire_cooking";
        } else if (serializer == RecipeSerializer.BLASTING_RECIPE) {
            id += "_from_blasting";
        } else if (serializer == RecipeSerializer.SMOKING_RECIPE) {
            id += "_from_smoking";
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        SimpleCookingRecipeBuilder builder = SimpleCookingRecipeBuilder.generic(ingredient, category, result, experience, duration, serializer, (AbstractCookingRecipe.Factory) factory);
        unlockedByHaving(builder, ingredient);
        builder.save(this.output, createRecipeId(id, result.asItem()));
    }

    public void netheriteUpgrade(RecipeCategory category, Ingredient input, ItemLike result) {
        Preconditions.checkNotNull(this.output);

        unlockedByHaving(smithing(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), input, Ingredient.of(Tags.Items.INGOTS_NETHERITE), category, result.asItem()), Tags.Items.INGOTS_NETHERITE).save(this.output, defaultRecipeId(result));
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
        if (ingredient.getValues().length == 1) {
            Ingredient.Value value = ingredient.getValues()[0];

            if (value instanceof Ingredient.ItemValue itemValue) {
                MKRecipeProvider.unlockedByHaving(builder, itemValue.item().getItem());
                return true;
            } else if (value instanceof Ingredient.TagValue tagValue) {
                MKRecipeProvider.unlockedByHaving(builder, tagValue.tag());
                return true;
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

    private static <T> T unlockedBy(T recipeBuilder, Criterion<?> criterion) {
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
        if (BuiltInRegistries.ITEM.containsValue(item.asItem())) {
            return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
        } else {
            throw new IllegalArgumentException("Item " + item.asItem() + " not found in items registry!");
        }
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
     *
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
