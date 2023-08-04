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

package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import thedarkcolour.modkit.data.model.SafeBlockModelProvider;

import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class MKBlockModelProvider extends BlockStateProvider {
    private final Lazy<MKItemModelProvider> itemModels;
    private final String modid;
    private final Consumer<MKBlockModelProvider> addBlockModels;

    private final SafeBlockModelProvider blockModels;

    @ApiStatus.Internal
    public MKBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, Lazy<MKItemModelProvider> itemModels, String modid, Logger logger, Consumer<MKBlockModelProvider> addBlockModels) {
        super(output, modid, existingFileHelper);
        this.itemModels = itemModels;
        this.modid = modid;
        this.addBlockModels = addBlockModels;
        this.blockModels = new SafeBlockModelProvider(output, modid, logger, existingFileHelper);
    }

    public ModelFile.UncheckedModelFile file(ResourceLocation resourceLoc) {
        return new ModelFile.UncheckedModelFile(resourceLoc);
    }

    public ModelFile.UncheckedModelFile modFile(String path) {
        return this.file(this.modBlock(path));
    }

    public ModelFile.UncheckedModelFile mcFile(String path) {
        return this.file(this.mcBlock(path));
    }

    public ResourceLocation modBlock(String name) {
        return this.modLoc("block/" + name);
    }

    public ResourceLocation mcBlock(String name) {
        return this.mcLoc("block/" + name);
    }

    public ResourceLocation key(Block block) {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block));
    }

    public String name(Block block) {
        return this.key(block).getPath();
    }

    @Override
    public BlockModelBuilder cubeAll(Block block) {
        return this.models().cubeAll(this.name(block), this.blockTexture(block));
    }

    @Override
    public void simpleBlockItem(Block block, ModelFile model) {
        this.itemModels.get().getBuilder(key(block).getPath()).parent(model);
    }

    /**
     * @deprecated Do not use this method, use the MKItemModelProvider from your IDataHelper
     */
    @Override
    @Deprecated
    public final ItemModelProvider itemModels() {
        try {
            if (!Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()).isInstance(this)) {
                throw new UnsupportedOperationException("Do not use MKBlockModelProvider to generate item models");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return super.itemModels();
    }

    @Override
    public BlockModelProvider models() {
        return this.blockModels;
    }

    @Override
    protected void registerStatesAndModels() {
        this.addBlockModels.accept(this);
    }

    @Override
    @NotNull
    public String getName() {
        return "ModKit Block Models for mod '" + this.modid + "'";
    }
}
