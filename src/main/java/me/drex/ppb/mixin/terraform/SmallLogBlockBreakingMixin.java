package me.drex.ppb.mixin.terraform;

import com.terraformersmc.terraform.wood.api.block.BareSmallLogBlock;
import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SmallLogBlock.class)
public abstract class SmallLogBlockBreakingMixin extends BareSmallLogBlock {
    private SmallLogBlockBreakingMixin(Properties properties) {
        super(properties);
    }

    // A leafy small log breaks in two steps, leaves first (LeafyLogBreaking), each at its own speed
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(SmallLogBlock.HAS_LEAVES)
            ? ((SmallLogBlock) (Object) this).leavesAt(level, pos).defaultBlockState().getDestroyProgress(player, level, pos)
            : super.getDestroyProgress(state, player, level, pos);
    }
}
