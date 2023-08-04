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

package thedarkcolour.modkit.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

public class SafeBlockModelBuilder extends BlockModelBuilder {
    private final Logger logger;

    public SafeBlockModelBuilder(ResourceLocation outputLocation, Logger logger, ExistingFileHelper existingFileHelper) {
        super(outputLocation, existingFileHelper);
        this.logger = logger;
    }

    // Ignore exceptions and generate models anyway
    @Override
    public BlockModelBuilder texture(String key, ResourceLocation texture) {
        try {
            return super.texture(key, texture);
        } catch (IllegalArgumentException e) {
            this.logger.error(e.getMessage());
            this.textures.put(key, texture.toString());
            return this;
        }
    }
}
