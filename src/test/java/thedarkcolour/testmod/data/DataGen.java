package thedarkcolour.testmod.data;

import net.minecraft.core.registries.Registries;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.data.loading.DatagenModLoader;
import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.testmod.TestMod;

/**
 * GatherDataEvent is fired on the MOD BUS!!!
 * Do NOT use {@code @Mod.EventBusSubscriber} to register to the mod bus. The annotation class loads
 * this class regardless of if ModKit is present, which can crash a player outside of dev.
 */
public final class DataGen {
    public static void gatherData(GatherDataEvent event) {
        DataHelper helper = new DataHelper(TestMod.ID, event);

        helper.createEnglish(true, English::addTranslations);
        helper.createBlockModels(BlockModels::addBlockModels);
        helper.createItemModels(true, true, false, ItemModels::addItemModels);
        helper.createRecipes(Recipes::addRecipes);
        helper.createTags(Registries.BLOCK, ModTags::addBlockTags);
        helper.createTags(Registries.ITEM, ModTags::addItemTags);
    }

    // Do not load this class outside of data gen
    static {
        if (!DatagenModLoader.isRunningDataGen()) {
            throw new RuntimeException("Class loaded!");
        }
    }
}
