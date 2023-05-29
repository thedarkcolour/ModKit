package thedarkcolour.testmod.data;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.MKBlockModelProvider;
import thedarkcolour.modkit.data.MKRecipeProvider;
import thedarkcolour.testmod.TestMod;

import java.util.function.Consumer;

// GatherDataEvent is fired on the MOD BUS!!! So specify mod bus.
@Mod.EventBusSubscriber(modid = TestMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataHelper helper = new DataHelper(TestMod.ID, event);

        helper.createEnglish(true, null);
        helper.createBlockModels(DataGen::addBlockModels);
        helper.createItemModels(true, true, false, null);
        helper.createRecipes(DataGen::addRecipes);
    }

    private static void addBlockModels(MKBlockModelProvider models) {
        models.simpleBlock(TestMod.ORANGE_BLOCK.get());
        models.simpleBlock(TestMod.RED_BLOCK.get());
    }

    private static void addRecipes(Consumer<FinishedRecipe> writer, MKRecipeProvider recipes) {
        recipes.storage3x3(TestMod.ORANGE_BLOCK.get(), TestMod.ORANGE.get());
    }
}
