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

package thedarkcolour.modkit.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thedarkcolour.modkit.ModKit;

public class InfinitePowerBlockEntity extends BlockEntity implements IEnergyStorage {
    public InfinitePowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModKit.INFINITE_POWER_TYPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
        if (!level.isClientSide) {
            Direction.stream().forEach(direction -> {
                BlockPos adjacentPos = pos.relative(direction);

                IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjacentPos, direction.getOpposite());
                if (energy != null) {
                    energy.receiveEnergy(Integer.MAX_VALUE, false);
                }
            });
        }
    }

    @Override
    public int receiveEnergy(int i, boolean b) {
        return 0;
    }

    @Override
    public int extractEnergy(int i, boolean b) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}