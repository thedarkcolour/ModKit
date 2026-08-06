/*
 * MIT License
 *
 * Copyright (c) 2025 thedarkcolour
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

package thedarkcolour.modkit.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

/**
 * How a {@link DataHelper} attaches a provider to the generator. The default is
 * {@link DataGenerator#addProvider}; override it through {@link DataHelper.Builder#addProvider} when a
 * mod needs to wrap, collect or redirect the providers a helper creates.
 */
@FunctionalInterface
public interface ProviderRegistrar {
    void addProvider(DataGenerator generator, boolean run, DataProvider provider);
}
