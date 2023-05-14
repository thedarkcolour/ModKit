package thedarkcolour.modkit.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFillWand extends Item {
    protected final Map<Player, Map<BlockPos, BlockState>> undoMap = new HashMap<>();

    public AbstractFillWand(Properties pProperties) {
        super(pProperties);
    }

    protected abstract MutableComponent getFillMessage();

    protected void fill(ItemStack stack, BlockState state, BlockPos pos, Level level, @Nullable Player player) {
        BlockPos startPos = NbtUtils.readBlockPos(stack.getTagElement("StartPos"));
        ImmutableMap.Builder<BlockPos, BlockState> builder = ImmutableMap.builder();

        for (BlockPos blockPos : BlockPos.betweenClosed(startPos, pos)) {
            BlockPos immutable = blockPos.immutable();
            builder.put(immutable, level.getBlockState(immutable));
            level.setBlock(immutable, state, 2);
        }

        if (player != null) {
            undoMap.put(player, builder.build());

            player.displayClientMessage(getFillMessage().append(String.format("(%d %d %d) to (%d %d %d)", startPos.getX(), startPos.getY(), startPos.getZ(), pos.getX(), pos.getY(), pos.getZ())), true);
        }
        stack.removeTagKey("StartPos");
    }

    protected boolean needsStartPos(ItemStack stack) {
        return stack.getTagElement("StartPos") == null;
    }

    protected void saveStartPos(ItemStack stack, BlockPos pos, @Nullable Player player) {
        stack.addTagElement("StartPos", NbtUtils.writeBlockPos(pos));
        if (player != null) {
            player.displayClientMessage(Component.literal(String.format("Starting position: %d %d %d", pos.getX(), pos.getY(), pos.getZ())), true);
        }
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand hand) {
        if (!pLevel.isClientSide) {
            if (pPlayer.isShiftKeyDown()) {
                pPlayer.getItemInHand(hand).removeTagKey("StartPos");
                pPlayer.displayClientMessage(Component.literal("Cleared start position"), true);
            } else if (undoMap.get(pPlayer) != null) {
                pPlayer.displayClientMessage(Component.literal("Hold to undo"), true);
                pPlayer.startUsingItem(hand);
            }
        }

        return InteractionResultHolder.pass(pPlayer.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();

        if (!level.isClientSide) {
            var stack = context.getItemInHand();
            var pos = context.getClickedPos();
            var player = context.getPlayer();

            if (player != null) {
                handleUse(level, stack, pos, player);
            }
        }

        return InteractionResult.SUCCESS;
    }

    protected abstract void handleUse(Level level, ItemStack stack, BlockPos pos, Player player);

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (pLivingEntity instanceof Player player) {
            var undoBlocks = undoMap.get(player);

            if (undoBlocks != null) {
                for (var entry : undoBlocks.entrySet()) {
                    pLevel.setBlock(entry.getKey(), entry.getValue(), 2);
                }
                player.displayClientMessage(Component.literal("Undo!"), true);
                undoMap.remove(player);
            }
        }

        return pStack;
    }
}
