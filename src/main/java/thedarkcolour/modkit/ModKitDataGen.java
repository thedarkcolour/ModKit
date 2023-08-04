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
        // Instead of manually adding data providers to the event, use the IDataHelper class
        var dataHelper = new DataHelper(ModKit.ID, event);
        dataHelper.createEnglish(true, ModKitDataGen::addNames);
        dataHelper.createItemModels(false, true, false, null);
    }

    // Although english generation gives appropriate names for most things, some are still done by hand
    private static void addNames(MKEnglishProvider english) {
        english.add("itemGroup.modkit", "ModKit");
    }
}
