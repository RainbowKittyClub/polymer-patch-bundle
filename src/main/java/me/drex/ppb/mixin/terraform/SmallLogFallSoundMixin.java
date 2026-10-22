package me.drex.ppb.mixin.terraform;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.drex.ppb.block.type.SmallLogPolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class SmallLogFallSoundMixin {
    // A fall onto a log leafed with other leaves sounds like those leaves
    @WrapOperation(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState ppb$storedLeavesFall(Level level, BlockPos pos, Operation<BlockState> original) {
        return SmallLogPolymerBlock.withStoredLeaves(original.call(level, pos), level, pos);
    }
}
