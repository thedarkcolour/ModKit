/*
 * MIT License
 *
 * Copyright (c) 2026 thedarkcolour
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
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.ItemDataMap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import static net.minecraft.data.recipes.SmithingTransformRecipeBuilder.smithing;

/**
 * ModKit's implementation of RecipeProvider, along with some static utility methods which can be found at the bottom of this file.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class MKRecipeProvider extends RecipeProvider {
    private static final VarHandle SHAPELESS_CRITERIA;
    private static final VarHandle SHAPED_CRITERIA;
    private static final VarHandle SHAPED_KEYS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        VarHandle shapeless;
        VarHandle shaped;
        VarHandle shapedKeys;

        try {
            shapeless = MethodHandles.privateLookupIn(ShapelessRecipeBuilder.class, lookup).findVarHandle(ShapelessRecipeBuilder.class, "criteria", Map.class);
            shaped = MethodHandles.privateLookupIn(ShapedRecipeBuilder.class, lookup).findVarHandle(ShapedRecipeBuilder.class, "criteria", Map.class);
            shapedKeys = MethodHandles.privateLookupIn(ShapedRecipeBuilder.class, lookup).findVarHandle(ShapedRecipeBuilder.class, "key", Map.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        SHAPELESS_CRITERIA = shapeless;
        SHAPED_CRITERIA = shaped;
        SHAPED_KEYS = shapedKeys;
    }

    private final String modid;
    private final BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes;
    @Nullable
    private RecipeOutput output;

    protected MKRecipeProvider(RecipeOutput output, HolderLookup.Provider lookupProvider, String modid, BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes) {
        super(lookupProvider, output);
        this.modid = modid;
        this.addRecipes = addRecipes;
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

    private static boolean isMissingCriterion(RecipeBuilder builder) {
        if (builder instanceof ShapelessRecipeBuilder) {
            return ((Map<?, ?>) SHAPELESS_CRITERIA.get(builder)).isEmpty();
        } else if (builder instanceof ShapedRecipeBuilder) {
            return ((Map<?, ?>) SHAPED_CRITERIA.get(builder)).isEmpty();
        } else {
            throw new IllegalArgumentException("Unable to determine if recipe is missing criterion");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ItemStack newItemStack(ItemLike item, int count, ItemDataMap data) {
        ItemStack stack = new ItemStack(item, count);

        for (ItemDataMap.Entry entry : data.entrySet()) {
            stack.set(entry.type(), entry.value());
        }

        return stack;
    }

    /**
     * @return The registry name/ID of the given item
     */
    @SuppressWarnings("deprecation")
    public static Identifier id(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key().identifier();
    }

    private static <T> T unlockedBy(T recipeBuilder, Criterion<?> criterion) {
        switch (recipeBuilder) {
            case RecipeBuilder b -> b.unlockedBy("has_item", criterion);
            case SmithingTrimRecipeBuilder b -> b.unlocks("has_item", criterion);
            case SmithingTransformRecipeBuilder b -> b.unlocks("has_item", criterion);
            default ->
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

    public static Ingredient lazyIngredient(Supplier<? extends ItemLike> item) {
        return ingredient(item.get());
    }

    @SafeVarargs
    public static Ingredient lazyIngredient(Supplier<? extends ItemLike>... items) {
        ItemLike[] values = new ItemLike[items.length];
        for (int i = 0; i < items.length; i++) {
            values[i] = items[i].get();
        }
        return ingredient(values);
    }

    private static IllegalArgumentException nonIngredientArgument(Object item) {
        return new IllegalArgumentException("Argument " + item + " is not instance of Ingredient, TagKey, or ItemLike");
    }

    @Override
    protected void buildRecipes() {
        this.addRecipes.accept(this.output, this);
    }

    public void conditional(String recipeId, List<ICondition> conditions, Consumer<RecipeOutput> addRecipes) {
        conditional(Identifier.fromNamespaceAndPath(this.modid, recipeId), conditions, addRecipes);
    }

    /**
     * Allows creation of conditional recipes.
     *
     * @param recipeId   The ID of the conditional recipe
     * @param conditions The list of conditions used for all recipe(s) added in addRecipes
     * @param addRecipes Add recipe(s) to the conditional recipe. Make sure you are using the Consumer from this lambda!
     */
    public void conditional(Identifier recipeId, List<ICondition> conditions, Consumer<RecipeOutput> addRecipes) {
        Preconditions.checkNotNull(this.output);
        Preconditions.checkArgument(!conditions.isEmpty(), "Cannot add a recipe with no conditions.");

        RecipeOutput outputWithConditions = this.output.withConditions(conditions.toArray(ICondition[]::new));

        pushRecipeOutput(outputWithConditions, addRecipes);
    }

    public void pushRecipeOutput(SimpleRecipeOutput newOutput, Consumer<RecipeOutput> action) {
        pushRecipeOutput((RecipeOutput) newOutput, action);
    }

    /**
     * This method temporarily changes the {@link RecipeOutput} used for the finished recipe writer.
     * By default, the recipe output used by MKRecipeProvider comes from the data generation event.
     * This method may be useful when an alternative behavior for handling FinishedRecipes generated by this class
     * is desired.
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

    /**
     * When you run into conflicting recipe names, put the conflicting recipe inside the runnable of this method and
     * pass the desired name of the recipe as name.
     * <p>
     * Example:
     * // one recipe that has the same name
     * recipes.renameRecipes(oldName -> oldName.withSuffix("_from_block", newWriter -> {
     * // another recipe that has the same name
     * });
     *
     * @param renaming The function responsible for renaming the recipes.
     * @param runnable Add your recipes here. You must use the recipe writer passed here INSTEAD of the original recipe writer variable.
     */
    public void renameRecipes(UnaryOperator<Identifier> renaming, Consumer<RecipeOutput> runnable) {
        Preconditions.checkNotNull(this.output);
        RecipeOutput oldWriter = this.output;

        pushRecipeOutput(new RecipeOutput() {
            @Override
            public Advancement.Builder advancement() {
                return oldWriter.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                oldWriter.includeRootAdvancement();
            }

            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                oldWriter.accept(createRecipeKey(renaming.apply(id.identifier())), recipe, advancement, conditions);
            }
        }, runnable);
    }

    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, 1, recipe);
    }

    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, int resultCount, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, resultCount, ItemDataMap.of(), recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, 1, recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, resultCount, ItemDataMap.of(), recipe);
    }

    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, ItemDataMap resultData, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(null, category, result, resultCount, resultData, recipe);
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
     * @param resultData  The data components of the result item
     * @param recipe      Function, usually a lambda, which defines the recipe layout by calling define and key on the recipe builder.
     */
    public void shapedCrafting(@Nullable String recipeId, RecipeCategory category, ItemLike result, int resultCount, ItemDataMap resultData, Consumer<ShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(this.output);

        ItemStack resultStack = newItemStack(result, resultCount, resultData);
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), category, resultStack);
        recipe.accept(builder);
        if (isMissingCriterion(builder)) {
            attemptAutoCriterion(builder);
        }

        ResourceKey<Recipe<?>> id = createRecipeKey(recipeId, builder.getResult());

        builder.save(this.output, id);
    }

    public Identifier defaultRecipeId(ItemLike result) {
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
    public Identifier createRecipeId(@Nullable String recipeId, ItemLike result) {
        if (recipeId != null) {
            if (recipeId.contains(":")) {
                return Identifier.parse(recipeId);
            } else {
                return Identifier.fromNamespaceAndPath(this.modid, recipeId);
            }
        } else {
            return Identifier.fromNamespaceAndPath(this.modid, MKRecipeProvider.path(result));
        }
    }

    public ResourceKey<Recipe<?>> createRecipeKey(@Nullable String recipeId, ItemLike result) {
        return ResourceKey.create(Registries.RECIPE, createRecipeId(recipeId, result));
    }

    public ResourceKey<Recipe<?>> createRecipeKey(Identifier recipeId) {
        return ResourceKey.create(Registries.RECIPE, recipeId);
    }

    /**
     * Simplest overload which accepts a category, result, and count.
     */
    public void shapelessCrafting(RecipeCategory category, ItemLike result, int resultCount, Object... ingredients) {
        shapelessCrafting(category, new ItemStack(result, resultCount), ingredients);
    }

    /**
     * Overload that accepts a group.
     */
    public void shapelessCrafting(RecipeCategory category, ItemLike result, int resultCount, @Nullable String group, Object... ingredients) {
        shapelessCrafting(category, new ItemStack(result, resultCount), ingredients);
    }

    /**
     * Overload that accepts an ID path.
     */
    public void shapelessCrafting(String path, RecipeCategory category, ItemLike result, int resultCount, Object... ingredients) {
        shapelessCrafting(Identifier.fromNamespaceAndPath(this.modid, path), category, result, resultCount, ingredients);
    }

    /**
     * Overload that accepts an item data map, which is like the old item NBT tag.
     */
    public void shapelessCrafting(RecipeCategory category, ItemLike result, int resultCount, ItemDataMap resultData, Object... ingredients) {
        shapelessCrafting(category, newItemStack(result, resultCount, resultData), ingredients);
    }

    /**
     * Overload that accepts an ID.
     */
    public void shapelessCrafting(Identifier id, RecipeCategory category, ItemLike result, int resultCount, Object... ingredients) {
        shapelessCrafting(id, category, new ItemStack(result, resultCount), null, ingredients);
    }

    /**
     * Overload that accepts an ItemStack instead of a result and count.
     */
    public void shapelessCrafting(RecipeCategory category, ItemStack result, Object... ingredients) {
        shapelessCrafting(category, result, null, null, ingredients);
    }

    /**
     * Overload that accepts a group and an ItemStack instead of a result and count.
     */
    public void shapelessCrafting(RecipeCategory category, ItemStack result, @Nullable String group, Object... ingredients) {
        shapelessCrafting(category, result, group, null, ingredients);
    }

    /**
     * Overload that accepts an ID path and recipe group.
     */
    public void shapelessCrafting(String path, RecipeCategory category, ItemLike result, int resultCount, @Nullable String group, Object... ingredients) {
        shapelessCrafting(Identifier.fromNamespaceAndPath(this.modid, path), category, result, resultCount, ingredients);
    }

    /**
     * Overload that accepts an ID and an ItemStack instead of a result and count.
     */
    public void shapelessCrafting(RecipeCategory category, ItemStack result, @Nullable Pair<String, Criterion<?>> unlockedBy, Object... ingredients) {
        shapelessCrafting(null, category, result, unlockedBy, ingredients);
    }

    public void shapelessCrafting(@Nullable Identifier id, RecipeCategory category, ItemStack result, @Nullable Pair<String, Criterion<?>> unlockedBy, Object... ingredients) {
        shapelessCrafting(id, category, result, null, unlockedBy, ingredients);
    }

    public void shapelessCrafting(RecipeCategory category, ItemStack result, @Nullable String group, @Nullable Pair<String, Criterion<?>> unlockedBy, Object... ingredients) {
        shapelessCrafting(null, category, result, group, unlockedBy, ingredients);
    }

    /**
     * Generates a shapeless recipe with a list of ingredients (can be a mix of ItemLike, Ingredient, and/or TagKey)
     * and attempts to also generate a recipe criterion so (hopefully) you don't need to call {@code unlockedBy}.
     * <p>
     * Additionally, it is possible to use {@link ObjectIntPair} or {@link IntObjectPair} containing one of the above
     * types to specify that the ingredient should appear multiple times (specified by the integer of the pair). This
     * helps avoid repetition of the same ingredient several times in the ingredients list.
     * <p>
     * There are many overloads that accept different variations and combinations of these arguments in the same order.
     * Generally, recipes start with an optional ID (string or Identifier), a category, a result (ItemStack or
     * pair of item + count), then an optional group name, an optional unlock criterion.
     * The last argument is always the list of ingredients.
     *
     * @param id          The ID to use for this recipe. If {@code null}, then one is chosen according to {@link RecipeBuilder#getDefaultRecipeId}.
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
    public void shapelessCrafting(@Nullable Identifier id, RecipeCategory category, ItemStack result, @Nullable String group, @Nullable Pair<String, Criterion<?>> unlockedBy, Object... ingredients) {
        Preconditions.checkNotNull(output);

        ShapelessRecipeBuilder shapeless = ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM), category, result);

        if (group != null) {
            shapeless.group(group);
        }

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

                switch (ingredient) {
                    case ItemLike itemLike -> {
                        shapeless.requires(itemLike);

                        if (noCriterion) {
                            unlockedByHaving(shapeless, itemLike);
                            noCriterion = false;
                        }
                    }
                    case TagKey tagKey -> {
                        shapeless.requires(tagKey);

                        if (noCriterion) {
                            unlockedByHaving(shapeless, tagKey);
                            noCriterion = false;
                        }
                    }
                    case Ingredient ing -> {
                        shapeless.requires(ing);

                        if (noCriterion) {
                            noCriterion = !unlockedByHaving(shapeless, ing);
                        }
                    }
                    default -> throw MKRecipeProvider.nonIngredientArgument(ingredient);
                }
            }

            if (noCriterion && isMissingCriterion(shapeless)) {
                throw new IllegalStateException("Argument list must contain one TagKey or ItemLike for adding automatic advancement criterion");
            }
        }

        if (id != null) {
            shapeless.save(output, createRecipeKey(id));
        } else {
            shapeless.save(output);
        }
    }

    @SuppressWarnings("unchecked")
    private void attemptAutoCriterion(ShapedRecipeBuilder builder) {
        for (Ingredient ingredient : ((Map<Character, Ingredient>) SHAPED_KEYS.get(builder)).values()) {
            if (unlockedByHaving(builder, ingredient)) {
                return;
            }
        }
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

        ShapelessRecipeBuilder fromStorage = ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.MISC, material, 9);
        unlockedByHaving(fromStorage, storage);
        fromStorage.requires(storage);
        fromStorage.save(this.output, createRecipeKey(id(material).withSuffix("_from_" + id(storage).getPath())));
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

    public void grid2x2(RecipeCategory category, ItemLike result, Ingredient ingredient) {
        grid2x2(category, result, 1, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        grid2x2(category, result, 1, ingredient);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, int resultCount, Ingredient ingredient) {
        grid2x2(category, result, resultCount, ingredient, null);
    }

    public void grid2x2(RecipeCategory category, ItemLike result, int resultCount, ItemLike ingredient) {
        grid2x2(category, result, resultCount, Ingredient.of(ingredient));
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

        shapedCrafting(category, result, resultCount, recipe -> {
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

        shapedCrafting(category, result, resultCount, recipe -> {
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

    public void woodenFence(ItemLike fence, ItemLike planks) {
        shapedCrafting(RecipeCategory.BUILDING_BLOCKS, fence, 3, recipe -> {
            recipe.define('#', Tags.Items.RODS_WOODEN);
            recipe.define('W', planks);
            recipe.pattern("W#W");
            recipe.pattern("W#W");
            recipe.group("wooden_fence");
        });
    }

    public void woodenFenceGate(ItemLike fenceGate, ItemLike planks) {
        shapedCrafting(RecipeCategory.BUILDING_BLOCKS, fenceGate, recipe -> {
            recipe.define('#', Tags.Items.RODS_WOODEN);
            recipe.define('W', planks);
            recipe.pattern("#W#");
            recipe.pattern("#W#");
            recipe.group("wooden_fence_gate");
        });
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

    public void special(String id, Function<CraftingBookCategory, Recipe<?>> factory) {
        special(Identifier.fromNamespaceAndPath(this.modid, id), factory);
    }

    /**
     * Used for special recipes like firework stars or butterfly breeding.
     *
     * @param id      The ID of this recipe.
     * @param factory The factory used to serialize the recipe result.
     */
    public void special(Identifier id, Function<CraftingBookCategory, Recipe<?>> factory) {
        Preconditions.checkNotNull(this.output);

        SpecialRecipeBuilder.special(factory).save(this.output, id.toString());
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
        builder.save(this.output, createRecipeKey(id, result.asItem()));
    }

    public void netheriteUpgrade(RecipeCategory category, Ingredient input, ItemLike result) {
        Preconditions.checkNotNull(this.output);

        unlockedByHaving(smithing(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), input, ingredient(Tags.Items.INGOTS_NETHERITE), category, result.asItem()), Tags.Items.INGOTS_NETHERITE).save(this.output, createRecipeKey(defaultRecipeId(result)));
    }

    /**
     * Takes in an Ingredient and tries to extract its Item or TagKey for making a recipe criterion.
     * This is necessary because an Ingredient cannot be used for normal recipe criterion.
     *
     * @param builder    The recipe builder to add a criterion to (can be RecipeBuilder or the smithing recipe builders)
     * @param ingredient The ingredient to try to use as a criterion
     * @return True if a criterion was added to the recipe
     */
    public boolean unlockedByHaving(Object builder, Ingredient ingredient) {
        if (ingredient.isEmpty() || ingredient.isCustom()) {
            return false;
        }

        LinkedHashSet<ItemLike> items = new LinkedHashSet<>();

        Optional<TagKey<Item>> tag = ingredient.getValues().unwrapKey();
        if (tag.isPresent()) {
            unlockedByHaving(builder, tag.get());
            return true;
        }

        for (var value : ingredient.getValues()) {
            items.add(value.value());
        }

        unlockedByHaving(builder, items.iterator().next());
        return true;
    }

    /**
     * Sets a recipe's unlockedBy criterion to InventoryChangeTrigger.TriggerInstance.has(TagKey),
     * which is protected and thus normally restricted to subclasses of RecipeProvider.
     *
     * @param recipeBuilder The recipe builder
     * @param item          The tag the player must have in their inventory to unlock the recipe
     * @return The recipe builder
     */
    public <T> T unlockedByHaving(T recipeBuilder, ItemLike item) {
        return unlockedBy(recipeBuilder, has(item));
    }

    public <T> T unlockedByHaving(T recipeBuilder, Holder<Item> holder) {
        return unlockedBy(recipeBuilder, has(holder));
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> has(Holder<Item> holder) {
        return inventoryTrigger(new ItemPredicate(Optional.of(HolderSet.direct(holder)), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY));
    }

    /**
     * Sets a recipe's unlockedBy criterion to InventoryChangeTrigger.TriggerInstance.has(TagKey),
     * which is protected and thus normally restricted to subclasses of RecipeProvider.
     *
     * @param recipeBuilder The recipe builder
     * @param tag           The tag the player must have in their inventory to unlock the recipe
     * @return The recipe builder
     */
    public <T> T unlockedByHaving(T recipeBuilder, TagKey<Item> tag) {
        return unlockedBy(recipeBuilder, has(tag));
    }

    public Ingredient ingredient(TagKey<Item> tag) {
        return tag(tag);
    }

    @SafeVarargs
    public final Ingredient ingredient(TagKey<Item>... tags) {
        return new CompoundIngredient(Arrays.stream(tags).map(this::ingredient).toList()).toVanilla();
    }

    /**
     * Creates an ingredient with a series of values, a combination of the following types:
     * <ul>
     *     <li>{@link ItemLike}</li>
     *     <li>{@link Supplier}&lt;? extends {@link ItemLike}&gt;</li>
     *     <li>{@link Holder}&lt;{@link Item}&gt;</li>
     *     <li>{@link HolderSet}&lt;{@link Item}&gt;</li>
     *     <li>{@link TagKey}&lt;{@link Item}&gt;</li>
     *     <li>{@link Ingredient}</li>
     * </ul>
     *
     * @param values The values the ingredient can match against
     * @return An ingredient with the specified values
     * @throws IllegalArgumentException If any element in {@code values} is not one of the permitted types listed above
     */
    @SuppressWarnings({"unchecked"})
    public Ingredient ingredient(Object... values) {
        int count = values.length;
        if (count == 0) {
            return Ingredient.of();
        }

        // Single input special cases - skip CompoundIngredient when possible
        if (count == 1) {
            Object value = values[0];
            return switch (value) {
                case ItemLike itemLike -> Ingredient.of(itemLike);
                case Supplier<?> supplier -> Ingredient.of((ItemLike) supplier.get());
                case Holder<?> holder -> Ingredient.of(HolderSet.direct((Holder<Item>) holder));
                case HolderSet<?> holderSet -> Ingredient.of((HolderSet<Item>) holderSet);
                case TagKey<?> tagKey -> ingredient((TagKey<Item>) tagKey);
                case Ingredient ingredient -> ingredient;
                default ->
                        throw new IllegalArgumentException("Invalid Ingredient value: " + value.getClass().getName());
            };
        }

        // Categorize values into groups
        List<Holder<Item>> holders = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        List<HolderSet<Item>> holderSets = new ArrayList<>();
        List<Ingredient> ingredients = new ArrayList<>();

        for (Object value : values) {
            switch (value) {
                case ItemLike itemLike -> holders.add(BuiltInRegistries.ITEM.wrapAsHolder(itemLike.asItem()));
                case Supplier<?> supplier ->
                        holders.add(BuiltInRegistries.ITEM.wrapAsHolder(((ItemLike) supplier.get()).asItem()));
                case Holder<?> holder -> holders.add((Holder<Item>) holder);
                case HolderSet<?> holderSet -> holderSets.add((HolderSet<Item>) holderSet);
                case TagKey<?> tagKey -> tags.add((TagKey<Item>) tagKey);
                case Ingredient ingredient -> ingredients.add(ingredient);
                default ->
                        throw new IllegalArgumentException("Invalid Ingredient value: " + value.getClass().getName());
            }
        }

        // If only holders (from ItemLike, Supplier, or Holder), produce a single simple ingredient
        if (tags.isEmpty() && holderSets.isEmpty() && ingredients.isEmpty()) {
            return Ingredient.of(HolderSet.direct(holders));
        }

        // Need CompoundIngredient - combine holders into one ingredient, then add each tag/holderset/ingredient separately
        List<Ingredient> parts = new ArrayList<>();

        if (!holders.isEmpty()) {
            parts.add(Ingredient.of(HolderSet.direct(holders)));
        }
        for (TagKey<Item> tag : tags) {
            parts.add(ingredient(tag));
        }
        for (HolderSet<Item> holderSet : holderSets) {
            parts.add(Ingredient.of(holderSet));
        }
        parts.addAll(ingredients);

        return new CompoundIngredient(parts).toVanilla();
    }

    /**
     * Allows using {@link #pushRecipeOutput(SimpleRecipeOutput, Consumer)} with just a lambda for the recipe output.
     */
    public interface SimpleRecipeOutput extends RecipeOutput {
        @Override
        default Advancement.Builder advancement() {
            return new Advancement.Builder();
        }

        void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe);

        @Override
        default void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementHolder advancementHolder, ICondition... conditions) {
            accept(key, recipe);
        }
    }

    public static class Runner implements DataProvider {
        private final PackOutput packOutput;
        private final CompletableFuture<HolderLookup.Provider> registries;
        private final String modid;
        private final BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes;

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, String modid, BiConsumer<RecipeOutput, MKRecipeProvider> addRecipes) {
            this.packOutput = packOutput;
            this.registries = registries;
            this.modid = modid;
            this.addRecipes = addRecipes;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return this.registries
                    .thenCompose(provider -> {
                        PackOutput.PathProvider recipePathProvider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                        PackOutput.PathProvider advancementPathProvider = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                        Set<ResourceKey<Recipe<?>>> set = Sets.newHashSet();
                        List<CompletableFuture<?>> list = new ArrayList<>();
                        RecipeOutput recipeOutput = new RecipeOutput() {
                            @Override
                            public void accept(ResourceKey<Recipe<?>> recipeKey, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                                if (!set.add(recipeKey)) {
                                    throw new IllegalStateException("Duplicate recipe " + recipeKey.identifier());
                                } else {
                                    this.saveRecipe(recipeKey, recipe, conditions);
                                    if (advancement != null) {
                                        this.saveAdvancement(advancement, conditions);
                                    }
                                }
                            }

                            @SuppressWarnings("removal")
                            @Override
                            public Advancement.Builder advancement() {
                                return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                            }

                            @Override
                            public void includeRootAdvancement() {
                                AdvancementHolder advancementholder = Advancement.Builder.recipeAdvancement()
                                        .addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                                        .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                                saveAdvancement(advancementholder);
                            }

                            private void saveRecipe(ResourceKey<Recipe<?>> recipeKey, Recipe<?> recipe) {
                                saveRecipe(recipeKey, recipe, new ICondition[0]);
                            }

                            private void saveRecipe(ResourceKey<Recipe<?>> recipeKey, Recipe<?> recipe, ICondition... conditions) {
                                list.add(
                                        DataProvider.saveStable(output, provider, Recipe.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(recipe, conditions)), recipePathProvider.json(recipeKey.identifier()))
                                );
                            }

                            private void saveAdvancement(AdvancementHolder advancement) {
                                saveAdvancement(advancement, new ICondition[0]);
                            }

                            private void saveAdvancement(AdvancementHolder advancement, ICondition... conditions) {
                                list.add(
                                        DataProvider.saveStable(
                                                output, provider, Advancement.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(advancement.value(), conditions)), advancementPathProvider.json(advancement.id())
                                        )
                                );
                            }
                        };
                        new MKRecipeProvider(recipeOutput, provider, this.modid, this.addRecipes).buildRecipes();
                        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                    });
        }

        @Override
        public String getName() {
            return "ModKit Recipes for mod '" + this.modid + "'";
        }
    }
}
