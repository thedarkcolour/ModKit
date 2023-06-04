package thedarkcolour.testmod.data;

import thedarkcolour.modkit.data.MKItemModelProvider;
import thedarkcolour.testmod.TestMod;

// Package private so that multiple mods can use these names without having a ton of autocomplete options
class ItemModels {
    static void addItemModels(MKItemModelProvider models) {
        models.exclude(TestMod.ORANGE);
    }
}
