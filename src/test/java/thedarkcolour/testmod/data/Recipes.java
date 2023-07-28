package thedarkcolour.testmod.data;

import net.minecraft.data.recipes.FinishedRecipe;
import thedarkcolour.modkit.data.MKRecipeProvider;
import thedarkcolour.testmod.TestMod;

import java.util.function.Consumer;

// Package private so that multiple mods can use these names without having a ton of autocomplete options
class Recipes {
    static void addRecipes(Consumer<FinishedRecipe> writer, MKRecipeProvider recipes) {
        recipes.storage3x3(TestMod.ORANGE_BLOCK.get(), TestMod.ORANGE.get());
    }
}
