/*
 * MIT License
 *
 * Copyright (c) 2026 thedarkcolour
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

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import thedarkcolour.modkit.MKUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * English language generation.
 * Can even generate names for modded types, just use {@link #addTranslationHandler(Class, Function)} and {@link #addRegistryForAutoTranslation(ResourceKey)}.
 */
@SuppressWarnings({"unchecked", "deprecation", "unused"})
public class MKEnglishProvider extends LanguageProvider {
    private static final Field FIELD_DATA;

    static {
        try {
            FIELD_DATA = LanguageProvider.class.getDeclaredField("data");
            FIELD_DATA.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reflect into LanguageProvider, overrides of generated names will not work!", e);
        }
    }

    private final String modid;
    private final Logger logger;
    private final boolean generateNames;
    @Nullable
    private final Consumer<MKEnglishProvider> addNames;
    private final Map<String, Component> data;
    private final Map<Class<?>, Function<Object, @Nullable String>> registryObjectHandlers;
    private final List<ResourceKey<? extends Registry<?>>> autoTranslatedRegistries;

    @ApiStatus.Internal
    public MKEnglishProvider(PackOutput output, String modid, Logger logger, boolean generateNames, @Nullable Consumer<MKEnglishProvider> addNames) {
        super(output, modid, "en_us");
        this.modid = modid;
        this.logger = logger;
        this.generateNames = generateNames;
        this.addNames = addNames;

        try {
            this.data = (Map<String, Component>) FIELD_DATA.get(this);
        } catch (IllegalAccessException ignored) {
            throw new IllegalStateException("Failed to create MKEnglishProvider");
        }

        // Default translation key handlers
        this.registryObjectHandlers = new HashMap<>();
        addTranslationHandler(Block.class, Block::getDescriptionId);
        addTranslationHandler(Item.class, Item::getDescriptionId);
        addTranslationHandler(EntityType.class, EntityType::getDescriptionId);
        addTranslationHandler(MobEffect.class, MobEffect::getDescriptionId);
        addTranslationHandler(Enchantment.class, MKEnglishProvider::getEnchantmentDescriptionId);
        addTranslationHandler(ItemStack.class, stack -> stack.getItem().getDescriptionId());
        addTranslationHandler(FluidType.class, FluidType::getDescriptionId);

        // Registries which will have names generated automatically
        this.autoTranslatedRegistries = new ArrayList<>();
        autoTranslatedRegistries.add(Registries.ITEM);
        autoTranslatedRegistries.add(Registries.BLOCK);
        autoTranslatedRegistries.add(Registries.ENTITY_TYPE);
        autoTranslatedRegistries.add(Registries.ENCHANTMENT);
        autoTranslatedRegistries.add(NeoForgeRegistries.Keys.FLUID_TYPES);
    }

    @Nullable
    public static String getEnchantmentDescriptionId(Enchantment enchantment) {
        if (enchantment.description().getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        } else {
            return null;
        }
    }

    @Override
    protected void addTranslations() {
        if (this.addNames != null) {
            this.addNames.accept(this);
        }

        if (this.generateNames) {
            for (ResourceKey<? extends Registry<?>> registryKey : this.autoTranslatedRegistries) {
                var optional = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookup(registryKey);

                if (optional.isPresent()) {
                    MutableInt i = new MutableInt();

                    try {
                        MKUtils.forModRegistry(optional.get(), this.modid, (id, obj) -> {
                            String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                            String key = getTranslationKey(obj);

                            if (key != null) {
                                if (!this.data.containsKey(key)) {
                                    add(key, name);
                                    i.increment();
                                }
                            } else {
                                this.logger.warn("Tried to automatically add translation \"{}\" for object {}, but was unable to determine translation key", name, obj);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        this.logger.error("No translation key handler registered by mod {} for registry {} (use MKEnglishProvider.addTranslationHandler)", this.modid, registryKey.identifier());
                        continue;
                    }

                    if (i.intValue() > 0) {
                        this.logger.info("Automatically generated {} names for mod {}'s entries in registry {}", i, this.modid, registryKey.identifier());
                    }
                } else {
                    this.logger.error("Failed to fetch registry {} for translation. You will have to translate these entries manually", registryKey.identifier());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "ModKit Language: en_us for mod '" + this.modid + "'";
    }

    @Override
    public void add(String key, String value) {
        Component literalValue = Component.literal(value);
        Component old = this.data.put(key, literalValue);
        if (old != null && !old.equals(literalValue)) {
            this.logger.info("Overridden/duplicate translation key \"{}\" (old: \"{}\" new: \"{}\")", key, old.getString(), value);
        }
    }

    /**
     * If you have a translatable object not included by default for adding translation keys,
     * add a mapping function here so that {@link #add(Object, String)} can properly add
     * translation keys for your specific type of registry object.
     *
     * @param type            The superclass to handle (ex. Block, Item)
     * @param translationKeys The mapping function, returns a translation key for the object (ex. Block::getDescriptionId)
     * @param <T>             The generic type for the registry
     */
    public <T> void addTranslationHandler(Class<T> type, Function<T, String> translationKeys) {
        this.registryObjectHandlers.put(type, (Function<Object, String>) translationKeys);
    }

    /**
     * If your mod adds its own registry, you can register it here so that English names
     * will be automatically generated for its members.
     * <p>
     * IMPORTANT: Make sure to call {@link #addTranslationHandler(Class, Function)} to register a translation key
     * handler for your registryKey objects, otherwise ModKit will not be able to translate them!
     *
     * @param registryKey The ID of the registry to iterate for automatically generating English names
     * @param <T>         The generic type for the registry
     */
    public <T> void addRegistryForAutoTranslation(ResourceKey<? extends Registry<T>> registryKey) {
        if (!this.generateNames) {
            this.logger.error("Tried to automatically generate English names for registryKey {}, but {} MKEnglishProvider has 'generateNames' set to false!", registryKey.identifier(), this.modid);
            throw new IllegalStateException("MKEnglishGenerator.generateNames is false");
        } else {
            this.autoTranslatedRegistries.add(registryKey);
        }
    }

    public void add(Object key, String name) {
        var translationKey = getTranslationKey(key);
        if (translationKey != null) {
            add(translationKey, name);
        } else {
            this.logger.warn("Tried to manually add translation \"{}\" for object {}, but was unable to determine translation key", name, key);
        }
    }

    public void addGeneric(Holder<?> key, String name) {
        add(key.value(), name);
    }

    @Nullable
    public String getTranslationKey(Object object) {
        for (var entry : this.registryObjectHandlers.entrySet()) {
            if (entry.getKey().isInstance(object)) {
                return entry.getValue().apply(object);
            }
        }

        throw new IllegalArgumentException("Unsupported registry object type for translation keys");
    }
}
