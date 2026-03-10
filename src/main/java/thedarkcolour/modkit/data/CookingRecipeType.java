package thedarkcolour.modkit.data;

import net.minecraft.world.item.crafting.*;

/**
 * Used in MKRecipeProvider to specify what kind of cooking recipe you want.
 * Replaces the RecipeSerializer.SMELTING, RecipeSerializer.CAMPFIRE_COOKING_RECIPE, etc. from old versions.
 *
 * @param idSuffix The suffix to add to each recipe ID created by this type (can be empty)
 * @param factory  Factory returning new instances of the recipe
 * @param <T>      The recipe class type
 */
public record CookingRecipeType<T extends AbstractCookingRecipe>(String idSuffix, AbstractCookingRecipe.Factory<T> factory) {
    public static final CookingRecipeType<SmeltingRecipe> SMELTING_RECIPE = new CookingRecipeType<>("", SmeltingRecipe::new);
    public static final CookingRecipeType<CampfireCookingRecipe> CAMPFIRE_COOKING_RECIPE = new CookingRecipeType<>("_from_campfire_cooking", CampfireCookingRecipe::new);
    public static final CookingRecipeType<BlastingRecipe> BLASTING_RECIPE = new CookingRecipeType<>("_from_blasting", BlastingRecipe::new);
    public static final CookingRecipeType<SmokingRecipe> SMOKING_RECIPE = new CookingRecipeType<>("_from_smoking", SmokingRecipe::new);
}
