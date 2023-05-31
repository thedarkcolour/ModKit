package thedarkcolour.testmod.data;

import thedarkcolour.modkit.data.MKBlockModelProvider;
import thedarkcolour.testmod.TestMod;

// Package private so that multiple mods can use these names without having a ton of autocomplete options
class BlockModels {
    static void addBlockModels(MKBlockModelProvider models) {
        models.simpleBlock(TestMod.ORANGE_BLOCK.get());
        models.simpleBlock(TestMod.RED_BLOCK.get());
    }
}
