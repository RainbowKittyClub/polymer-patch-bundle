package me.drex.ppb.mixin.terraform;

import com.terraformersmc.terraform.wood.api.block.BareSmallLogBlock;
import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SmallLogBlock.class)
public abstract class SmallLogBlockMixin extends BareSmallLogBlock {
    @Shadow
    @Final
    private Block leaves;

    private SmallLogBlockMixin(Properties properties) {
        super(properties);
    }

    // A leafy small log reaches clients as its leaves, so it sounds like them on the server too
    @Override
    protected SoundType getSoundType(BlockState state) {
        return state.getValue(SmallLogBlock.HAS_LEAVES) ? this.leaves.defaultBlockState().getSoundType() : super.getSoundType(state);
    }
}
