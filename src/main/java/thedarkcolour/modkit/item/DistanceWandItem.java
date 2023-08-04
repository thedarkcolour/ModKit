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

import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class DistanceWandItem extends Item {
    public DistanceWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();

        if (!level.isClientSide) {
            var stack = ctx.getItemInHand();
            var pos = ctx.getClickedPos();
            var player = ctx.getPlayer();

            if (player == null) return InteractionResult.PASS;

            var startPosNbt = stack.getTagElement("StartPos");
            if (startPosNbt != null) {
                var start = NbtUtils.readBlockPos(startPosNbt);

                var dx = pos.getX() == start.getX() ? 0 : Math.abs(pos.getX() - start.getX()) + 1;
                var dy = pos.getY() == start.getY() ? 0 : Math.abs(pos.getY() - start.getY()) + 1;
                var dz = pos.getZ() == start.getZ() ? 0 : Math.abs(pos.getZ() - start.getZ()) + 1;

                player.displayClientMessage(Component.literal(String.format("Distance (XYZ): (%d, %d, %d)", dx, dy, dz)), false);
                stack.removeTagKey("StartPos");
            } else {
                stack.addTagElement("StartPos", NbtUtils.writeBlockPos(pos));
                player.displayClientMessage(Component.literal(String.format("Measurement starting position: (%d %d %d)", pos.getX(), pos.getY(), pos.getZ())), true);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
