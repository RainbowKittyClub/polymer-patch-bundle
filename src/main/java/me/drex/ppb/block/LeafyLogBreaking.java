package me.drex.ppb.block;

import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import me.drex.ppb.mixin.terraform.SmallLogBlockAccessor;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Breaking a leafy small log takes only its leaves, as if breaking that leaves block, and leaves the bare log
 * standing until it is broken itself.
 */
public final class LeafyLogBreaking {
    private LeafyLogBreaking() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(state.getBlock() instanceof SmallLogBlock block) || !state.getValue(SmallLogBlock.HAS_LEAVES)
                || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            BlockState leaves = ((SmallLogBlockAccessor) block).ppb$getLeaves().defaultBlockState()
                .trySetValue(LeavesBlock.PERSISTENT, true);
            // A client that broke the block itself (creative, instant mining) already showed the leaves breaking
            serverLevel.levelEvent(PredictedBreakEffects.isBrokenByClient(serverPlayer, pos) ? serverPlayer : null,
                LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(leaves));
            if (!serverPlayer.isCreative()) {
                ItemStack tool = serverPlayer.getMainHandItem();
                Block.dropResources(leaves, serverLevel, pos, null, serverPlayer, tool);
                tool.mineBlock(serverLevel, leaves, pos, serverPlayer);
                serverPlayer.awardStat(Stats.BLOCK_MINED.get(leaves.getBlock()));
                serverPlayer.causeFoodExhaustion(0.005F);
            }
            serverLevel.setBlock(pos, state.setValue(SmallLogBlock.HAS_LEAVES, false), Block.UPDATE_ALL);
            return false;
        });
    }
}
