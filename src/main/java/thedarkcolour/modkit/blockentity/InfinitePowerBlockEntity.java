/*
 * MIT License
 *
 * Copyright (c) 2025 thedarkcolour
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

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.ModKit;

public class InfinitePowerBlockEntity extends BlockEntity implements IEnergyStorage {
	private final LazyOptional<IEnergyStorage> energyCap;

	public InfinitePowerBlockEntity(BlockPos pos, BlockState state) {
		super(ModKit.INFINITE_POWER_TYPE.get(), pos, state);

		this.energyCap = LazyOptional.of(() -> this);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
		if (!level.isClientSide) {
			Direction.stream().forEach(direction -> {
				BlockPos adjacentPos = pos.relative(direction);

				BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
				if (blockEntity != null) {
					blockEntity.getCapability(ForgeCapabilities.ENERGY)
							.ifPresent(energy -> energy.receiveEnergy(Integer.MAX_VALUE, false));
				}
			});
		}
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (this.remove && cap == ForgeCapabilities.ENERGY) {
			return this.energyCap.cast();
		}
		return super.getCapability(cap, side);
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
