package me.drex.ppb.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.drex.ppb.block.DetailedBlocks;
import me.drex.ppb.block.PredictedBreakEffects;
import me.drex.ppb.block.type.ChorusStandIn;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.jetbrains.annotations.Nullable;
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

    // 26.3 mining dust is drawn from each client's own block, purple for a chorus stand-in; those players get
    // factorytools' hit particles of the real block instead (ChorusStandIn#shouldSendHitParticles)
    @WrapOperation(
        method = "levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V")
    )
    private void ppb$skipChorusHitDust(PlayerList playerList, @Nullable Player except, double x, double y, double z, double range, ResourceKey<Level> dimension, Packet<?> packet, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int type, @Local(argsOnly = true) BlockPos pos) {
        ServerLevel level = (ServerLevel) (Object) this;
        ChorusStandIn standIn = type == LevelEvent.PARTICLES_DESTROY_PROGRESS || type == LevelEvent.PARTICLES_AND_SOUND_DESTROY_PROGRESS
            ? ChorusStandIn.at(level, pos) : null;
        if (standIn == null) {
            original.call(playerList, except, x, y, z, range, dimension, packet);
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player != except && player.distanceToSqr(x, y, z) < range * range && !DetailedBlocks.shows(player, standIn.kind())) {
                player.connection.send(packet);
            }
        }
    }
}
