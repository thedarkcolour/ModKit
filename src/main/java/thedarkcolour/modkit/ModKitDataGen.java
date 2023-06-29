package thedarkcolour.modkit;

import net.minecraftforge.data.event.GatherDataEvent;
import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.MKEnglishProvider;

/**
 * For a more detailed example of data generation, go to the "test" source set of ModKit on
 * <a href="https://github.com/thedarkcolour/ModKit/tree/1.20.1/src/test/java/thedarkcolour/testmod">GitHub</a>
 */
final class ModKitDataGen {
    static void gatherData(GatherDataEvent event) {
        // Instead of manually adding data providers to the event, use the DataHelper class
        var dataHelper = new DataHelper(ModKit.ID, event);
        dataHelper.createEnglish(true, ModKitDataGen::addNames);
        dataHelper.createItemModels(false, true, false, null);
    }

    // Although english generation gives appropriate names for most things, some are still done by hand
    private static void addNames(MKEnglishProvider english) {
        english.add("itemGroup.modkit", "ModKit");
    }
}
