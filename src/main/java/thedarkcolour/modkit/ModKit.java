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

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thedarkcolour.modkit.item.ClearWandItem;
import thedarkcolour.modkit.item.CloneWandItem;
import thedarkcolour.modkit.item.DistanceWandItem;
import thedarkcolour.modkit.item.FillWandItem;

@Mod(ModKit.ID)
public class ModKit {
    public static final String ID = "modkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<Item> FILL_WAND = ITEMS.register("fill_wand", () -> new FillWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CLEAR_WAND = ITEMS.register("clear_wand", () -> new ClearWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DISTANCE_WAND = ITEMS.register("distance_wand", () -> new DistanceWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CLONE_WAND = ITEMS.register("clone_wand", () -> new CloneWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    static {
        CREATIVE_TABS.register(ID, () -> Util.make(new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0), builder -> {
            builder.icon(() -> new ItemStack(CLONE_WAND.get()));
            builder.title(Component.translatable("itemGroup.modkit"));
            builder.displayItems((params, output) -> {
                output.accept(FILL_WAND.get());
                output.accept(CLEAR_WAND.get());
                output.accept(DISTANCE_WAND.get());
                output.accept(CLONE_WAND.get());
            });
            builder.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
        }).build());
    }

    public ModKit() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        modBus.addListener(ModKit::postRegistry);
        modBus.addListener(ModKitDataGen::gatherData);
        modBus.addListener(EventPriority.LOWEST, ModKit::postCreativeTabs);
    }

    private static void postRegistry(FMLLoadCompleteEvent event) {
        MKUtils.forInDevMods(modInfo -> {
            MKUtils.forModRegistry(Registries.BLOCK, modInfo.getModId(), (id, block) -> {
                if (Item.BY_BLOCK.get(block) == null) {
                    ModKit.LOGGER.warn("Block '{}' has no block item", id);
                }
            });
            // Maybe something about entities without spawn eggs next?
        });
    }

    /**
     * Triggers upon first opening the Creative Menu. Warns about registered items which do not
     * show in any creative tab, which means they will not show in JEI.
     */
    private static void postCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        var allTabs = CreativeModeTabs.allTabs();

        // only print errors on the last tab
        if (allTabs.indexOf(event.getTab()) + 1 == allTabs.size()) {
            MKUtils.forInDevMods(modInfo -> MKUtils.forModRegistry(Registries.ITEM, modInfo.getModId(), (id, item) -> {
                for (var tab : allTabs) {
                    for (var entry : tab.getDisplayItems()) {
                        if (entry.getItem() == item) {
                            return;
                        }
                    }
                }

                ModKit.LOGGER.warn("Item '{}' is not found in any creative tabs (will not show in JEI!)", id);
            }));
        }
    }
}
