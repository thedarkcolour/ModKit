package thedarkcolour.testmod.data;

import net.minecraftforge.registries.ForgeRegistries;
import thedarkcolour.modkit.data.MKEnglishProvider;

class English {
    static void addTranslations(MKEnglishProvider lang) {
        // Since MobEffect.getDescriptionId is handled by default in ModKit,
        // there is no need to call addTranslationHandler. If you are using
        // a modded registry, (ex. "Bee Species") you must add one yourself.
        lang.addRegistryForAutoTranslation(ForgeRegistries.MOB_EFFECTS);
    }
}
