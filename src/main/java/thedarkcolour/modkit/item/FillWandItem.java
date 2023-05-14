package thedarkcolour.modkit.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FillWandItem extends AbstractFillWand {
    public FillWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected MutableComponent getFillMessage() {
        return Component.literal("Filled blocks from ");
    }

    @Override
    protected void handleUse(Level level, ItemStack stack, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) {
            var state = level.getBlockState(pos);
            stack.addTagElement("FillBlock", NbtUtils.writeBlockState(state));
            player.displayClientMessage(Component.literal("Set block to " + state.getBlock()), true);
        } else {
            if (needsStartPos(stack)) {
                saveStartPos(stack, pos, player);
            } else {
                CompoundTag savedFillBlock = stack.getTagElement("FillBlock");

                if (savedFillBlock == null) {
                    player.displayClientMessage(Component.literal("No filler block"), true);
                } else {
                    fill(stack, NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), savedFillBlock), pos, level, player);
                    player.getCooldowns().addCooldown(this, 5);
                }
            }
        }
    }
}
