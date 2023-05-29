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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.NbtShapedRecipeBuilder;
import thedarkcolour.modkit.data.recipe.NbtShapelessRecipeBuilder;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MKRecipeProvider extends RecipeProvider {
    private final BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes;
    @Nullable
    private Consumer<FinishedRecipe> writer;

    public MKRecipeProvider(PackOutput output, BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes) {
        super(output);
        this.addRecipes = addRecipes;
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        this.writer = writer;
        this.addRecipes.accept(writer, this);
        this.writer = null;
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, 1, recipe);
    }

    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, int resultCount, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, resultCount, null, recipe);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt, Consumer<NbtShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(writer);

        NbtShapedRecipeBuilder builder = new NbtShapedRecipeBuilder(category, result, resultCount, resultNbt);
        recipe.accept(builder);
        if (builder.isMissingCriterion()) {
            builder.attemptAutoCriterion();
        }
        builder.save(writer, recipeId);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(RecipeCategory category, ItemLike result, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, 1, recipe);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, Consumer<NbtShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, resultCount, null, recipe);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt, Consumer<NbtShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(writer);

        NbtShapedRecipeBuilder builder = new NbtShapedRecipeBuilder(category, result, resultCount, resultNbt);
        recipe.accept(builder);
        builder.save(writer);
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
     * @param ingredients Can be ItemLike, Ingredient, or TagKey<Item>
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

        ShapelessRecipeBuilder toStorage = new ShapelessRecipeBuilder(RecipeCategory.MISC, storage, 1);
        unlockedByHaving(toStorage, material);
        toStorage.requires(material, 9);
        toStorage.save(writer);

        ShapelessRecipeBuilder fromStorage = new ShapelessRecipeBuilder(RecipeCategory.MISC, material, 9);
        unlockedByHaving(fromStorage, storage);
        fromStorage.requires(storage);
        fromStorage.save(writer, id(material).withSuffix("_from_storage"));
    }

    public static ResourceLocation id(ItemLike item) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.asItem()));
    }

    public static <T extends RecipeBuilder> T unlockedByHaving(T builder, ItemLike item) {
        builder.unlockedBy("has_item", has(item));
        return builder;
    }

    public static <T extends RecipeBuilder> T unlockedByHaving(T builder, TagKey<Item> tag) {
        builder.unlockedBy("has_item", has(tag));
        return builder;
    }

    /**
     * Takes in an Ingredient and tries to extract its Item or TagKey for making a recipe criterion.
     * This is necessary because an Ingredient cannot be used for normal recipe criterion.
     *
     * @param builder    The recipe builder to add a criterion to
     * @param ingredient The ingredient to try to use as a criterion
     * @return Whether a criterion was added or not
     */
    public static boolean unlockedByHaving(RecipeBuilder builder, Ingredient ingredient) {
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
