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

package thedarkcolour.modkit.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class CloneWandItem extends AbstractFillWand {
    private final Map<Player, ImmutableMap<BlockPos, BlockState>> structureMap = new HashMap<>();

    public CloneWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected MutableComponent getFillMessage() {
        return Component.literal("Placed the structure ");
    }

    @Override
    protected void handleUse(Level level, ItemStack stack, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) {
            var startPosNbt = stack.getTagElement("StartPos");
            if (startPosNbt == null) {
                saveStartPos(stack, pos, player);
            } else {
                var builder = ImmutableMap.<BlockPos, BlockState>builder();
                var start = NbtUtils.readBlockPos(startPosNbt);

                for (var mutable : BlockPos.betweenClosed(start, pos)) {
                    builder.put(mutable.subtract(start), level.getBlockState(mutable));
                }

                structureMap.put(player, builder.build());
                player.displayClientMessage(Component.literal(String.format("Saved blocks from (%d %d %d) to (%d %d %d)", start.getX(), start.getY(), start.getZ(), pos.getX(), pos.getY(), pos.getZ())), true);
                stack.removeTagKey("StartPos");
            }
        } else {
            if (!structureMap.containsKey(player)) return;
            var pos2BlockState = structureMap.get(player).entrySet();
            var builder = ImmutableMap.<BlockPos, BlockState>builder();

            for (var entry : pos2BlockState) {
                var state = entry.getValue();
                var posOffset = entry.getKey().offset(pos);
                builder.put(posOffset, level.getBlockState(posOffset));

                level.setBlockAndUpdate(posOffset, state);
            }

            undoMap.put(player, builder.build());
            player.getCooldowns().addCooldown(this, 25);
            player.displayClientMessage(Component.literal(String.format("Cloned structure anchored at (%d %d %d)", pos.getX(), pos.getY(), pos.getZ())), true);
        }
    }
}
