package thedarkcolour.modkit.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
            if (needsStartPos(stack)) {
                saveStartPos(stack, pos, player);
            } else {
                ImmutableMap.Builder<BlockPos, BlockState> builder = ImmutableMap.builder();
                var start = NbtUtils.readBlockPos(stack.getTagElement("StartPos"));

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
            player.displayClientMessage(Component.literal(String.format("Cloned structure anchored at (%d %d %d)", pos.getX(), pos.getY(), pos.getZ())), true);
        }
    }
}
