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

package thedarkcolour.modkit.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import thedarkcolour.modkit.ModKit;

public class DistanceWandItem extends Item {
    public DistanceWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();

        if (!level.isClientSide) {
            var stack = ctx.getItemInHand();
            var pos = ctx.getClickedPos();
            var player = ctx.getPlayer();

            if (player == null) return InteractionResult.PASS;

            var startPos = stack.get(ModKit.START_POS_COMPONENT.get());
            if (startPos != null) {
                var dx = pos.getX() == startPos.getX() ? 0 : Math.abs(pos.getX() - startPos.getX()) + 1;
                var dy = pos.getY() == startPos.getY() ? 0 : Math.abs(pos.getY() - startPos.getY()) + 1;
                var dz = pos.getZ() == startPos.getZ() ? 0 : Math.abs(pos.getZ() - startPos.getZ()) + 1;

                player.displayClientMessage(Component.literal(String.format("Distance (XYZ): (%d, %d, %d)", dx, dy, dz)), false);
                stack.remove(ModKit.START_POS_COMPONENT.get());
            } else {
                stack.set(ModKit.START_POS_COMPONENT.get(), pos);
                player.displayClientMessage(Component.literal(String.format("Measurement starting position: (%d %d %d)", pos.getX(), pos.getY(), pos.getZ())), true);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
