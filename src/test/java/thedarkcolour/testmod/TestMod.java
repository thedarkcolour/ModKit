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

package thedarkcolour.testmod;

import net.minecraft.world.effect.AttackDamageMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thedarkcolour.testmod.data.DataGen;

@Mod(TestMod.ID)
public class TestMod {
    public static final String ID = "testmod";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TestMod.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TestMod.ID);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TestMod.ID);

    public static final RegistryObject<Block> RED_BLOCK = BLOCKS.register("red_block", () -> new Block(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.HONEY_BLOCK))) ;
    public static final RegistryObject<Item> RED_BLOCK_ITEM = ITEMS.register("red_block", () -> new BlockItem(RED_BLOCK.get(), new Item.Properties().stacksTo(3)));

    public static final RegistryObject<Item> ORANGE = ITEMS.register("orange", () -> new Item(new Item.Properties().stacksTo(6)));
    public static final RegistryObject<Block> ORANGE_BLOCK = BLOCKS.register("orange_block", () -> new Block(BlockBehaviour.Properties.of().strength(12.0f).sound(SoundType.HONEY_BLOCK))) ;
    public static final RegistryObject<Item> ORANGE_BLOCK_ITEM = ITEMS.register("orange_block", () -> new BlockItem(ORANGE_BLOCK.get(), new Item.Properties().stacksTo(10)));

    public static final RegistryObject<MobEffect> BRUISE = MOB_EFFECTS.register("bruise", () -> new MobEffect(MobEffectCategory.HARMFUL, 1) {});

    public TestMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MOB_EFFECTS.register(modBus);

        modBus.addListener(DataGen::gatherData);
    }
}
