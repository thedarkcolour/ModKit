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

import com.google.common.base.Preconditions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class SafeBlockModelProvider extends BlockModelProvider {
    private final Logger logger;

    public SafeBlockModelProvider(PackOutput output, String modid, Logger logger, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
        this.logger = logger;
    }

    @Override
    protected void registerModels() {}

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf();
    }

    public ResourceLocation extendWithFolder(ResourceLocation loc) {
        if (loc.getPath().contains("/")) {
            return loc;
        }
        return new ResourceLocation(loc.getNamespace(), folder + "/" + loc.getPath());
    }

    @Override
    public BlockModelBuilder getBuilder(String path) {
        Preconditions.checkNotNull(path, "Path must not be null");
        ResourceLocation outputLoc = extendWithFolder(path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(modid, path));
        this.existingFileHelper.trackGenerated(outputLoc, MODEL);
        return generatedModels.computeIfAbsent(outputLoc, loc -> new SafeBlockModelBuilder(loc, logger, existingFileHelper));
    }

    @Override
    public BlockModelBuilder nested() {
        return new SafeBlockModelBuilder(new ResourceLocation("dummy:dummy"), logger, existingFileHelper);
    }

    @Override
    public BlockModelBuilder withExistingParent(String name, ResourceLocation parent) {
        try {
            return super.withExistingParent(name, parent);
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            return getBuilder(name).parent(new ModelFile.UncheckedModelFile(parent));
        }
    }
}
