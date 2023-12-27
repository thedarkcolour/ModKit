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
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ApiStatus.Internal
public class MKUtils {
    // only works for vanilla registries
    public static <T> void forModRegistry(ResourceKey<? extends Registry<T>> registryKey, String modid, BiConsumer<ResourceLocation, T> consumer) {
        forModRegistry(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(registryKey), modid, consumer);
    }

    public static <T> void forModRegistry(Registry<T> registry, String modid, BiConsumer<ResourceLocation, T> consumer) {
        for (var entry : registry.entrySet()) {
            var id = entry.getKey().location();

            if (id.getNamespace().equals(modid)) {
                consumer.accept(id, entry.getValue());
            }
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
