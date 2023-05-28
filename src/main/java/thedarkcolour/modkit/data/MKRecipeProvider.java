package thedarkcolour.modkit.data;

import com.google.common.base.Preconditions;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.recipe.NbtShapelessRecipeBuilder;

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
    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(recipeId, category, result, 1, recipe);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(String recipeId, RecipeCategory category, ItemLike result, int resultCount, Consumer<ShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(writer);

        ShapedRecipeBuilder builder = new ShapedRecipeBuilder(category, result, resultCount);
        recipe.accept(builder);
        builder.save(writer, recipeId);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(RecipeCategory category, ItemLike result, Consumer<ShapedRecipeBuilder> recipe) {
        shapedCrafting(category, result, 1, recipe);
    }

    // Make sure that you call "unlockedBy" in your recipe lambda
    public void shapedCrafting(RecipeCategory category, ItemLike result, int resultCount, Consumer<ShapedRecipeBuilder> recipe) {
        Preconditions.checkNotNull(writer);

        ShapedRecipeBuilder builder = new ShapedRecipeBuilder(category, result, resultCount);
        recipe.accept(builder);
        builder.save(writer);
    }
    public void shapelessCrafting(RecipeCategory category, ItemLike result, int resultCount, Object... ingredients) {
        shapelessCrafting(category, new ItemStack(result, resultCount, null), ingredients);
    }


    /**
     * Generates a shapeless recipe with a list of ingredients (can be a mix of ItemLike, Ingredient, and/or TagKey)
     * Make sure that you call "unlockedBy" in your recipe lambda
     * @param category
     * @param result
     * @param ingredients Can be ItemLike, Ingredient, or TagKey<Item>
     *
     * @throws IllegalArgumentException if any element of {@code ingredients} is not ItemLike, Ingredient, or TagKey
     */
    @SuppressWarnings("unchecked")
    private void shapelessCrafting(RecipeCategory category, ItemStack result, Object... ingredients) {
        Preconditions.checkNotNull(writer);

        NbtShapelessRecipeBuilder shapeless = new NbtShapelessRecipeBuilder(category, result.getItem(), result.getCount(), result.getTag());
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
            } else {
                throw nonIngredientArgument(ingredient);
            }
        }

        if (noCriterion) {
            throw new IllegalStateException("Argument list must contain one TagKey or ItemLike for adding advancement criterion");
        }
    }

    /**
     * Template for recipes which convert ingot <---> block. Also works for nugget <---> ingot.
     * Two recipes are generated by this method, but the recipe to convert from storage back
     * into material has the id "[modid]:[material]_from_storage" to avoid conflicts.
     *
     * @param storage The result of the 3x3 recipe (iron block from ingots, iron ingot from nuggets, etc.)
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
        fromStorage.save(writer, name(material) + "_from_storage");
    }

    public String name(ItemLike item) {
        var i = item.asItem();
        return ForgeRegistries.ITEMS.getKey(i).getPath();
    }

    public <T extends RecipeBuilder> T unlockedByHaving(T builder, ItemLike item) {
        builder.unlockedBy("has_item", has(item));
        return builder;
    }

    public <T extends RecipeBuilder> T unlockedByHaving(T builder, TagKey<Item> tag) {
        builder.unlockedBy("has_item", has(tag));
        return builder;
    }

    private IllegalArgumentException nonIngredientArgument(Object item) {
        return new IllegalArgumentException("Argument " + item + " is not instance of Ingredient, TagKey, or ItemLike");
    }
}
