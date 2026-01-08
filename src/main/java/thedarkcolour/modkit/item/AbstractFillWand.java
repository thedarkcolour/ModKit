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

package thedarkcolour.modkit.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.ModKit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractFillWand extends Item {
    protected final Map<Player, Map<BlockPos, BlockState>> undoMap = new HashMap<>();

    public AbstractFillWand(Properties properties) {
        super(properties);
    }

    protected abstract MutableComponent getFillMessage();

    protected void fill(ItemStack stack, BlockState state, BlockPos pos, Level level, @Nullable Player player) {
        var startPos = stack.get(ModKit.START_POS_COMPONENT);

        if (startPos != null) {
            var builder = ImmutableMap.<BlockPos, BlockState>builder();

            for (var blockPos : BlockPos.betweenClosed(startPos, pos)) {
                var immutable = blockPos.immutable();
                builder.put(immutable, level.getBlockState(immutable));
                level.setBlock(immutable, state, 2);
            }

            if (player != null) {
                undoMap.put(player, builder.build());

                player.displayClientMessage(getFillMessage().append(String.format("(%d %d %d) to (%d %d %d)", startPos.getX(), startPos.getY(), startPos.getZ(), pos.getX(), pos.getY(), pos.getZ())), true);
            }
            stack.remove(ModKit.START_POS_COMPONENT);
        }
    }

    protected void saveStartPos(ItemStack stack, BlockPos pos, @Nullable Player player) {
        stack.set(ModKit.START_POS_COMPONENT, pos);
        if (player != null) {
            player.displayClientMessage(Component.literal(String.format("Starting position: %d %d %d", pos.getX(), pos.getY(), pos.getZ())), true);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity living) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                player.getItemInHand(hand).remove(ModKit.START_POS_COMPONENT.get());
                player.displayClientMessage(Component.literal("Cleared start position"), true);
            } else if (undoMap.get(player) != null) {
                player.displayClientMessage(Component.literal("Hold to undo"), true);
                player.startUsingItem(hand);
            }
        }

        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();

        if (!level.isClientSide) {
            var stack = ctx.getItemInHand();
            var pos = ctx.getClickedPos();
            var player = ctx.getPlayer();

            if (player != null) {
                handleUse(level, stack, pos, player);
            }
        }

        return InteractionResult.SUCCESS;
    }

    protected abstract void handleUse(Level level, ItemStack stack, BlockPos pos, Player player);

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            var undoBlocks = undoMap.get(player);

            if (undoBlocks != null) {
                for (var entry : undoBlocks.entrySet()) {
                    level.setBlock(entry.getKey(), entry.getValue(), 2);
                }
                player.displayClientMessage(Component.literal("Undo!"), true);
                undoMap.remove(player);
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
        var startPos = stack.get(ModKit.START_POS_COMPONENT.get());
        if (startPos != null) {
            tooltip.add(Component.literal("Start Position: (" + startPos.getX() + ", " + startPos.getY() + ", " + startPos.getZ() + ")"));
        } else {
            tooltip.add(Component.literal("Tip: Hold sneak click in the air to undo last operation").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return stack.get(ModKit.START_POS_COMPONENT.get()) == null ? super.getName(stack) : Component.translatable(this.getDescriptionId(stack)).append("*");
    }
}
