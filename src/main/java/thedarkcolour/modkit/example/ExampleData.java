package thedarkcolour.modkit.example;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thedarkcolour.modkit.ModKit;
import thedarkcolour.modkit.data.DataHelper;

@Mod.EventBusSubscriber(modid = ModKit.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExampleData {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var dataHelper = new DataHelper(ModKit.ID, event);
        dataHelper.createEnglish(true, null);
        dataHelper.createItemModels(false, true, false, null);
    }
}
