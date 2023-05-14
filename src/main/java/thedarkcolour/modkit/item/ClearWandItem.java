package thedarkcolour.modkit.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class ClearWandItem extends AbstractFillWand {
    public ClearWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected MutableComponent getFillMessage() {
        return Component.literal("Cleared blocks from ");
    }

    @Override
    protected void handleUse(Level level, ItemStack stack, BlockPos pos, Player player) {
        if (needsStartPos(stack)) {
            saveStartPos(stack, pos, player);
        } else {
            player.getCooldowns().addCooldown(this, 5);
            fill(stack, Blocks.AIR.defaultBlockState(), pos, level, player);
        }
    }
}
