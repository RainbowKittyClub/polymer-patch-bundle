package me.drex.ppb.mixin.terraform;

import com.llamalad7.mixinextras.sugar.Local;
import me.drex.ppb.block.type.SmallLogPolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Entity.class)
public abstract class SmallLogStepSoundMixin {
    // Steps on a log leafed with other leaves sound like those leaves, for every entity's own step sound
    @ModifyArg(method = "walkingStepSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playStepSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"), index = 1)
    private BlockState ppb$storedLeavesStep(BlockState onState, @Local(argsOnly = true) BlockPos onPos) {
        return SmallLogPolymerBlock.withStoredLeaves(onState, ((Entity) (Object) this).level(), onPos);
    }
}
