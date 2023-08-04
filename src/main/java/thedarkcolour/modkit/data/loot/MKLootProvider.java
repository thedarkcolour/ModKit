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

package thedarkcolour.modkit.data.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MKLootProvider extends LootTableProvider {
    private final List<SubProviderEntry> providers = new ArrayList<>();

    public MKLootProvider(PackOutput output) {
        super(output, Set.of(), null);
    }

    @Override
    public List<SubProviderEntry> getTables() {
        return providers;
    }
}
