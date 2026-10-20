package me.drex.ppb.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.drex.ppb.block.PredictedBreakEffects;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @ModifyArg(
        method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V"),
        index = 0
    )
    private Player ppb$skipPredictedBreakSound(Player except) {
        ServerPlayer skipped = PredictedBreakEffects.skippedBySound();
        return except == null && skipped != null ? skipped : except;
    }

    @WrapOperation(
        method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDIDDDDDDLnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket$RandomizationType;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z")
    )
    private boolean ppb$skipPredictedBreakParticles(ServerLevel level, ServerPlayer player, boolean overrideLimiter, double x, double y, double z, Packet<?> packet, Operation<Boolean> original) {
        return !PredictedBreakEffects.skipsParticles(player) && original.call(level, player, overrideLimiter, x, y, z, packet);
    }
}
