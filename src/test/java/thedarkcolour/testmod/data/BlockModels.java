/*
 * MIT License
 *
 * Copyright (c) 2023 thedarkcolour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

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
