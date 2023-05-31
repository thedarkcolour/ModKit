package thedarkcolour.testmod.data;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.MKBlockModelProvider;
import thedarkcolour.testmod.TestMod;

/**
 * GatherDataEvent is fired on the MOD BUS!!! So specify mod bus.
 * <p>
 * For the methods referenced in your createBlah methods, they MUST be declared in a
 * different class than where {@link #gatherData} is located to avoid classloading ModKit
 * classes when this class is registered to the event bus, because even if data gen isn't
 * running (ex. for players) this class must be classloaded to check for SubscribeEvent
 * methods. If any other methods in the class take in a ModKit class as a parameter, like
 * {@link BlockModels#addBlockModels} then each parameter type will also be classloaded,
 * causing ModKit classes to load for players outside of dev and thus crashing.
 */
@Mod.EventBusSubscriber(modid = TestMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataHelper helper = new DataHelper(TestMod.ID, event);

        helper.createEnglish(true, null);
        helper.createBlockModels(BlockModels::addBlockModels);
        helper.createItemModels(true, true, false, null);
        // These methods should be declared in separate classes to avoid classloading ModKit if data isn't running
        helper.createRecipes(Recipes::addRecipes);
    }
}
