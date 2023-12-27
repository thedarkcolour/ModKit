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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import thedarkcolour.modkit.MKUtils;
import thedarkcolour.modkit.data.model.SafeItemModelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class MKItemModelProvider extends ModelProvider<SafeItemModelBuilder> {
    private final Logger logger;
    private final boolean generate3dBlockItems;
    private final boolean generate2dItems;
    private final boolean generateSpawnEggs;
    @Nullable
    private final Consumer<MKItemModelProvider> addItemModels;
    private final List<ResourceLocation> excluded = new ArrayList<>();

    @ApiStatus.Internal
    public MKItemModelProvider(PackOutput output,
                                  ExistingFileHelper helper,
                                  String modid,
                                  Logger logger,
                                  boolean generate3dBlockItems,
                                  boolean generate2dItems,
                                  boolean generateSpawnEggs,
                                  @Nullable Consumer<MKItemModelProvider> addItemModels) {
        super(output, modid, "item", (outputLoc, efh) -> new SafeItemModelBuilder(outputLoc, logger, efh), helper);

        this.logger = logger;
        this.generate3dBlockItems = generate3dBlockItems;
        this.generate2dItems = generate2dItems;
        this.generateSpawnEggs = generateSpawnEggs;
        this.addItemModels = addItemModels;
    }

    /**
     * If any of {@link #generate2dItems}, {@link #generate3dBlockItems}, {@link #generateSpawnEggs} are true,
     * this method prevents the specified item
     * @param item Will have no models automatically generated by ModKit (can still add them in #addItemModels)
     */
    public void exclude(ItemLike item) {
        excluded.add(extendWithFolder(itemId(item)));
    }

    public SafeItemModelBuilder generic2d(ItemLike item) {
        return generic2d(itemId(item));
    }

    /**
     * Makes a 2d single layer item like hopper, gold ingot, or redstone dust item models
     */
    public SafeItemModelBuilder generic2d(ResourceLocation itemId) {
        return layer0(itemId, "item/generated");
    }

    public SafeItemModelBuilder handheld(ItemLike item) {
        return handheld(itemId(item.asItem()));
    }

    /**
     * Makes a 2d single layer item with special transformations like the pickaxe or sword models.
     */
    public SafeItemModelBuilder handheld(ResourceLocation itemId) {
        return layer0(itemId, "item/handheld");
    }

    public SafeItemModelBuilder layer0(ResourceLocation itemId, String parentName) {
        String path = itemId.getPath();

        return getBuilder(path)
                .parent(new ModelFile.UncheckedModelFile(parentName)) // handheld
                .texture("layer0", new ResourceLocation(itemId.getNamespace(), "item/" + path));
    }

    /**
     * Makes a 3d cube of a block for item model
     */
    public SafeItemModelBuilder generic3d(ItemLike item) {
        ResourceLocation id = itemId(item.asItem());
        String path = id.getPath();
        return withExistingParent(path, new ResourceLocation(id.getNamespace(), "block/" + path));
    }

    public SafeItemModelBuilder generic3d(ResourceLocation id) {
        String path = id.getPath();
        return withExistingParent(path, new ResourceLocation(id.getNamespace(), "block/" + path));
    }

    private SafeItemModelBuilder spawnEgg(ItemLike item) {
        return spawnEgg(itemId(item));
    }

    private SafeItemModelBuilder spawnEgg(ResourceLocation itemId) {
        return getBuilder(itemId.getPath()).parent(new ModelFile.UncheckedModelFile("item/template_spawn_egg"));
    }

    @Override
    public SafeItemModelBuilder withExistingParent(String name, ResourceLocation parent) {
        try {
            return super.withExistingParent(name, parent);
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            return getBuilder(name).parent(new ModelFile.UncheckedModelFile(parent));
        }
    }

    public ResourceLocation extendWithFolder(ResourceLocation rl) {
        if (rl.getPath().contains("/")) {
            return rl;
        }
        return new ResourceLocation(rl.getNamespace(), folder + "/" + rl.getPath());
    }

    public ResourceLocation itemId(ItemLike item) {
        if (BuiltInRegistries.ITEM.containsValue(item.asItem())) {
            return BuiltInRegistries.ITEM.getKey(item.asItem());
        } else {
            throw new IllegalStateException("Item " + item.asItem() + " does not exist in item registry");
        }
    }

    @Override
    public String getName() {
        return "ModKit Item Models for mod '" + modid + "'";
    }

    @Override
    protected void registerModels() {
        if (generate3dBlockItems || generate2dItems || generateSpawnEggs) {
            MKUtils.forModRegistry(Registries.ITEM, modid, (id, item) -> {
                if (generate3dBlockItems && item instanceof BlockItem) {
                    generic3d(id);
                } else if (generateSpawnEggs && item instanceof SpawnEggItem) {
                    spawnEgg(id);
                } else if (generate2dItems) {
                    if (item instanceof ShovelItem || item instanceof SwordItem || item instanceof HoeItem || item instanceof AxeItem || item instanceof PickaxeItem) {
                        handheld(id);
                    } else {
                        generic2d(id);
                    }
                }
            });
        }
        if (addItemModels != null) {
            addItemModels.accept(this);
        }
        for (var exclusion : excluded) {
            generatedModels.remove(exclusion);
        }
    }
}
