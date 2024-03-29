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

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thedarkcolour.modkit.ModKit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Use this in your event handler for GatherDataEvent. To avoid requiring ModKit in-game,
 * GatherDataEvent should be in a separate class from mod code. See the examples package.
 * For a more detailed example, check the "src/test/" directory on ModKit's GitHub.
 * <p>
 * Methods which specify what to generate follow the naming scheme "createBlah" and always
 * have a consumer (usually nullable) as their last parameter which gets run at the appropriate
 * time during data generation. For example, while you'd normally write translation code in
 * LanguageProvider#addTranslations, you would write it in the consumer you pass as the last
 * argument for {@link #createEnglish}, either with a method reference to a static method
 * "addTranslations" in your own data generation class. If you do not need any additional
 * translations aside from what's automatically generated by ModKit, you may simply pass null.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class DataHelper {
    protected final String modid;
    protected final GatherDataEvent event;
    protected final Logger logger;
    protected final Map<ResourceKey<?>, MKTagsProvider<?>> tags;

    @Nullable
    protected MKEnglishProvider english;
    @Nullable
    protected MKItemModelProvider itemModels;
    @Nullable
    protected MKBlockModelProvider blockModels;
    @Nullable
    protected MKRecipeProvider recipes;
    @Nullable
    protected BiFunction<MKEnglishProvider, PackOutput, List<DataProvider>> addModonomiconBooks;

    public DataHelper(String modid, GatherDataEvent event) {
        this.modid = modid;
        this.event = event;
        this.logger = LoggerFactory.getLogger(ModKit.ID + "/" + modid);
        this.tags = new HashMap<>();
    }

    /**
     * Generates English language translations for your mod.
     *
     * @param generateNames   Whether to automatically generate names based on registry names.
     *                        For example, "minecraft:gold_ingot" would become "Gold Ingot".
     *                        Cases where this does not work is if your registry names do not follow
     *                        proper convention, or if you use acronyms: "projecte:emc_gun" would be "Emc Gun"
     *                        instead of "EMC Gun" and "mymod:exampleblock" would be "Exampleblock" instead of "Example Block"
     * @param addTranslations If not null, a consumer ran in LanguageProvider.addTranslations AFTER names are autogenerated,
     *                        so you can add in names that were generated incorrectly or names for things like Creative Tabs.
     */
    public MKEnglishProvider createEnglish(boolean generateNames, @Nullable Consumer<MKEnglishProvider> addTranslations) {
        this.checkNotCreated(this.english, "English language");

        this.english = new MKEnglishProvider(event.getGenerator().getPackOutput(), this.modid, this.logger, generateNames, addTranslations);

        if (addModonomiconBooks != null) {
            for (DataProvider book : addModonomiconBooks.apply(this.english, this.event.getGenerator().getPackOutput())) {
                this.event.getGenerator().addProvider(true, book);
            }
        }

        this.event.getGenerator().addProvider(this.event.includeClient(), this.english);

        return this.english;
    }

    /**
     * Use this to register your Modonomicon books before the language generation is created.
     *
     * @param customEnglish Whether you are calling {{@link #createEnglish(boolean, Consumer)}} later. If false,
     *                      this method will use an MKEnglishProvider that doesn't generate names.
     * @param addBooks      Create your book providers here, return them as a list.
     */
    public void createModonomiconBooks(boolean customEnglish, BiFunction<MKEnglishProvider, PackOutput, List<DataProvider>> addBooks) {
        this.addModonomiconBooks = addBooks;

        if (!customEnglish) {
            this.createEnglish(false, null);
        }
    }

    /**
     * Generates item models for your mod. If you also have Block Models, call this AFTER calling {@link #createBlockModels}
     *
     * @param generate3dBlockItems If true, BlockItems are given generic 3D item models (ex. dirt, diamond block)
     * @param generate2dItems      If true, non-BlockItems are given generic 2D item models (ex. gold ingot)
     *                             and SwordItem/PickaxeItem/ShovelItem/etc. items will be given handheld item models (ex. wooden pickaxe)
     * @param generateSpawnEggs    If true, SpawnEggItems are given spawn egg item models
     * @param addItemModels        Function (nullable) to add/override generated models for items. If you are using generate3dBlockItems
     *                             or generate2dItems and you want to change which model an item is generated with, do so in
     *                             this function.
     */
    public MKItemModelProvider createItemModels(boolean generate3dBlockItems, boolean generate2dItems, boolean generateSpawnEggs, @Nullable Consumer<MKItemModelProvider> addItemModels) {
        this.checkNotCreated(this.itemModels, "Item models");

        this.itemModels = new MKItemModelProvider(this.event.getGenerator().getPackOutput(), this.event.getExistingFileHelper(), this.modid, this.logger, generate3dBlockItems, generate2dItems, generateSpawnEggs, addItemModels);
        this.event.getGenerator().addProvider(this.event.includeClient(), this.itemModels);

        return this.itemModels;
    }

    /**
     * Generates block models for your mod. The MKBlockModelProvider provides some template models
     * you can use, but if you need anything more complicated it's best to write your own methods and
     * use them in the method you pass in for addBlockModels.
     *
     * @param addBlockModels Non-null function which receives the MKBlockModelProvider, which inherits methods from
     *                       BlockStateProvider and has some other methods to generate models.
     */
    public MKBlockModelProvider createBlockModels(Consumer<MKBlockModelProvider> addBlockModels) {
        this.checkNotCreated(this.blockModels, "Block models");

        if (this.itemModels != null) {
            // Ex. Item models which use block models as parents will log errors that those block models don't exist,
            // because data providers run in creation order so block models wouldn't have generated yet.
            this.logger.warn("Item model generation was added BEFORE block model generation; this is incorrect, expect some false alarm errors");
        }

        // Lazy is used so that createItemModels is called automatically if your mod isn't using it
        Lazy<MKItemModelProvider> lazyItemModels = Lazy.of(() -> {
            if (this.itemModels == null) {
                this.createItemModels(false, false, false, null);
                this.event.getGenerator().addProvider(this.event.includeClient(), this.itemModels);
            }
            return this.itemModels;
        });

        this.blockModels = new MKBlockModelProvider(this.event.getGenerator().getPackOutput(), this.event.getExistingFileHelper(), lazyItemModels, this.modid, this.logger, addBlockModels);
        this.event.getGenerator().addProvider(this.event.includeClient(), this.blockModels);

        return this.blockModels;
    }

    /**
     * Generates recipes of all kinds for your mod.
     *
     * @param addRecipes Non-null function which receives the finished recipe writer and MKRecipeProvider, which has
     *                   built-in methods for common recipe types. If you need something more advanced, you may write
     *                   methods using the given finished recipe writer and call them in your addRecipes function.
     */
    public MKRecipeProvider createRecipes(BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes) {
        this.checkNotCreated(this.recipes, "Recipes");

        this.recipes = new MKRecipeProvider(this.event.getGenerator().getPackOutput(), this.modid, addRecipes);
        this.event.getGenerator().addProvider(this.event.includeServer(), this.recipes);

        return this.recipes;
    }

    /**
     * Generates tags for a specific registry. For item tags, you may use the copy() method
     * to copy equivalent block tags into your item tags.
     *
     * @param registry The registry to generate tags for
     * @param addTags  A function that takes in the tag provider and a holder lookup provider in order to generate tags.
     * @param <T>      The type of objects to generate tags for
     * @return The tag provider, not sure what you'd use this for.
     */
    public <T> MKTagsProvider<T> createTags(ResourceKey<? extends Registry<T>> registry, BiConsumer<MKTagsProvider<T>, HolderLookup.Provider> addTags) {
        this.checkNotCreated(this.tags.get(registry), "Tags for " + registry.location());

        var provider = new MKTagsProvider<>(this, registry, addTags);
        this.tags.put(registry, provider);
        this.event.getGenerator().addProvider(this.event.includeServer(), provider);

        return provider;
    }
    /**
     * Alternative method which omits the often unused HolderLookup.Provider parameter.
     *
     * @see #createTags(ResourceKey, BiConsumer)
     */
    public <T> MKTagsProvider<T> createTags(ResourceKey<? extends Registry<T>> registry, Consumer<MKTagsProvider<T>> addTags) {
        return this.createTags(registry, (tags, lookup) -> addTags.accept(tags));
    }

    private void checkNotCreated(@Nullable Object obj, String provider) {
        if (obj != null) {
            throw new IllegalStateException(provider + " generation already created!");
        }
    }
}
