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

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MKUtils {
    // only works for vanilla registries
    public static <T> void forModRegistry(ResourceKey<? extends Registry<T>> registryKey, String modid, BiConsumer<ResourceLocation, T> consumer) {
        forModRegistry(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(registryKey), modid, consumer);
    }

    public static <T> void forModRegistry(Registry<T> registry, String modid, BiConsumer<ResourceLocation, T> consumer) {
        // Iterate in a deterministic order sorted by id. MappedRegistry#entrySet is backed by a HashMap
        // keyed on ResourceKey, whose identity-based hashCode makes iteration order vary between JVM runs.
        // Sorting keeps generated data (e.g. auto-generated lang names) stable across datagen runs.
        List<Map.Entry<ResourceKey<T>, T>> entries = new ArrayList<>();
        for (var entry : registry.entrySet()) {
            if (entry.getKey().location().getNamespace().equals(modid)) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.getKey().location()));
        for (var entry : entries) {
            consumer.accept(entry.getKey().location(), entry.getValue());
        }
    }

    public static void forInDevMods(Consumer<IModInfo> action) {
        for (IModFileInfo modsToml : ModList.get().getModFiles()) {
            for (IModInfo modInfo : modsToml.getMods()) {
                if (!modsToml.getFile().getFilePath().toAbsolutePath().toString().contains(".jar")) {
                    action.accept(modInfo);
                }
            }
        }
    }
}
