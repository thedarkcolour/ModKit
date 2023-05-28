package thedarkcolour.modkit.example;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thedarkcolour.modkit.ModKit;
import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.MKEnglishProvider;
import thedarkcolour.modkit.data.MKRecipeProvider;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = ModKit.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExampleData {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // Instead of manually adding data providers to the event, use the DataHelper class
        var dataHelper = new DataHelper(ModKit.ID, event);
        // Although english generation gives proper names for most things,
        dataHelper.createEnglish(true, ExampleData::addNames);
        dataHelper.createItemModels(false, true, false, null);
        dataHelper.createRecipes(ExampleData::addRecipes);
    }

    private static void addNames(MKEnglishProvider english) {
        english.add("itemGroup.modkit", "ModKit");
    }

    private static void addRecipes(Consumer<FinishedRecipe> writer, MKRecipeProvider recipes) {
        //recipes.shapedCrafting(RecipeCategory.MISC, );
    }
}
