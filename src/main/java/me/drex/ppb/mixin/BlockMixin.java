package me.drex.ppb.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.drex.ppb.block.PredictedBreakEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BlockMixin {
    @WrapOperation(
        method = "playerWillDestroy",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;spawnDestroyParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V")
    )
    private void ppb$skipPredictedBreakEffects(Block block, Level level, Player player, BlockPos pos, BlockState state, Operation<Void> original) {
        PredictedBreakEffects predicted = PredictedBreakEffects.of(level, player, pos, state);
        if (predicted == null) {
            original.call(block, level, player, pos, state);
        } else {
            ScopedValue.where(PredictedBreakEffects.CURRENT, predicted).run(() -> original.call(block, level, player, pos, state));
        }
    }
}
