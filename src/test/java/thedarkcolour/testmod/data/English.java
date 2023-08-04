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

import net.minecraft.core.registries.Registries;
import thedarkcolour.modkit.data.MKEnglishProvider;

class English {
    static void addTranslations(MKEnglishProvider lang) {
        // Since MobEffect.getDescriptionId is handled by default in ModKit,
        // there is no need to call addTranslationHandler. If you are using
        // a modded registry, (ex. "Bee Species") you must add one yourself.
        lang.addRegistryForAutoTranslation(Registries.MOB_EFFECT);
    }
}
