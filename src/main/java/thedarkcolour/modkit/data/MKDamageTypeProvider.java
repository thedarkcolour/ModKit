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

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MKDamageTypeProvider extends JsonCodecProvider<DamageType> {
    private final Consumer<MKDamageTypeProvider> addTypes;
    private final HashMap<Identifier, DamageTypeBuilder> types = new HashMap<>();

    public MKDamageTypeProvider(PackOutput output, String modid, CompletableFuture<HolderLookup.Provider> lookupProvider, Consumer<MKDamageTypeProvider> addTypes) {
        super(output, PackOutput.Target.DATA_PACK, "damage_type", DamageType.DIRECT_CODEC, lookupProvider, modid);
        this.addTypes = addTypes;
    }

    public DamageTypeBuilder add(ResourceKey<DamageType> type) {
        return this.types.computeIfAbsent(type.identifier(), key -> new DamageTypeBuilder(this.modid + '.' + type.identifier().getPath()));
    }

    @Override
    protected void gather() {
        this.addTypes.accept(this);
        this.types.forEach((id, builder) -> unconditional(id, builder.createType()));
    }

    public static class DamageTypeBuilder {
        private final String msgId;

        private DamageScaling scaling = DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER;
        private float exhaustion = 0.0f;
        private DamageEffects effects = DamageEffects.HURT;
        private DeathMessageType deathMessageType = DeathMessageType.DEFAULT;

        public DamageTypeBuilder(String msgId) {
            this.msgId = msgId;
        }

        public DamageTypeBuilder scaling(DamageScaling scaling) {
            this.scaling = scaling;
            return this;
        }

        public DamageTypeBuilder exhaustion(float exhaustion) {
            this.exhaustion = exhaustion;
            return this;
        }

        public DamageTypeBuilder effects(DamageEffects effects) {
            this.effects = effects;
            return this;
        }

        public DamageTypeBuilder deathMessageType(DeathMessageType deathMessageType) {
            this.deathMessageType = deathMessageType;
            return this;
        }

        public DamageType createType() {
            return new DamageType(this.msgId, this.scaling, this.exhaustion, this.effects, this.deathMessageType);
        }
    }
}