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

package thedarkcolour.modkit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thedarkcolour.modkit.block.InfinitePowerBlock;
import thedarkcolour.modkit.blockentity.InfinitePowerBlockEntity;
import thedarkcolour.modkit.item.*;

import java.util.Set;

@Mod(ModKit.ID)
public class ModKit {
    public static final String ID = "modkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredBlock<Block> INFINITE_POWER = BLOCKS.registerBlock("infinite_power", InfinitePowerBlock::new);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfinitePowerBlockEntity>> INFINITE_POWER_TYPE = BLOCK_ENTITIES.register("infinite_power", () -> new BlockEntityType<>(InfinitePowerBlockEntity::new, Set.of(INFINITE_POWER.get())));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<BlockItem> INFINITE_POWER_ITEM = ITEMS.registerSimpleBlockItem(INFINITE_POWER, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<Item> FILL_WAND = ITEMS.registerItem("fill_wand", FillWandItem::new, props -> props.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> CLEAR_WAND = ITEMS.registerItem("clear_wand", ClearWandItem::new, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> DISTANCE_WAND = ITEMS.registerItem("distance_wand", DistanceWandItem::new, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> CLONE_WAND = ITEMS.registerItem("clone_wand", CloneWandItem::new, props -> props.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> KILL_WAND = ITEMS.registerItem("kill_wand", KillWandItem::new, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> START_POS_COMPONENT = DATA_COMPONENTS.registerComponentType("start_pos", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> FILL_BLOCK_COMPONENT = DATA_COMPONENTS.registerComponentType("fill_block", builder -> builder.persistent(BlockState.CODEC).networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)));

    static {
        CREATIVE_TABS.register(ID, () -> Util.make(new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0), builder -> {
            builder.icon(() -> new ItemStack(CLONE_WAND.get()));
            builder.title(Component.translatable("itemGroup.modkit"));
            builder.displayItems((params, output) -> {
                output.accept(INFINITE_POWER_ITEM.get());
                output.accept(FILL_WAND.get());
                output.accept(CLEAR_WAND.get());
                output.accept(DISTANCE_WAND.get());
                output.accept(CLONE_WAND.get());
                output.accept(KILL_WAND.get());
            });
            builder.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
        }).build());
    }

    public ModKit(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        modBus.addListener(ModKit::postRegistry);
        modBus.addListener(ModKitDataGen::gatherData);
        modBus.addListener(EventPriority.LOWEST, ModKit::postCreativeTabs);
        modBus.addListener(ModKit::registerCapabilities);
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
     * todo FIX
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

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, INFINITE_POWER_TYPE.get(), (power, face) -> power);
    }




}
