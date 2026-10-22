package me.drex.ppb.mixin.terraform;

import com.llamalad7.mixinextras.sugar.Local;
import me.drex.ppb.block.type.SmallLogPolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({LivingEntity.class, ServerPlayer.class})
public abstract class SmallLogFallParticlesMixin {
    // Landing (and a mace smash) on a log leafed with other leaves throws up those leaves
    @ModifyArg(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"), index = 1)
    private BlockState ppb$storedLeavesLanding(BlockState onState, @Local(argsOnly = true) BlockPos pos) {
        return SmallLogPolymerBlock.withStoredLeaves(onState, ((Entity) (Object) this).level(), pos);
    }
}
