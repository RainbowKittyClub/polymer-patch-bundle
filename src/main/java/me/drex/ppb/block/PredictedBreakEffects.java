package me.drex.ppb.block;

import eu.pb4.factorytools.api.block.CustomBreakingParticleBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import me.drex.ppb.block.type.StatePolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Break effects the breaking player's client already played itself, since it broke the block on its own
 * (creative or instant mining). factorytools sends its break sound and particles to every player, so the
 * matching ones skip this player while the block's break effects are spawned.
 */
public record PredictedBreakEffects(ServerPlayer player, boolean sound, boolean particles) {
    public static final ScopedValue<PredictedBreakEffects> CURRENT = ScopedValue.newInstance();
    private static final @Nullable MethodHandle IS_BROKEN_BY_CLIENT = findIsBrokenByClient();

    public static @Nullable PredictedBreakEffects of(Level level, Player player, BlockPos pos, BlockState state) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
            || !(PolymerSyncedObject.getSyncedObject(BuiltInRegistries.BLOCK, state.getBlock()) instanceof CustomBreakingParticleBlock block)
            || !isBrokenByClient(serverPlayer, pos)) {
            return null;
        }

        BlockState clientState = PolymerBlockUtils.getPolymerBlockState(state, serverPlayer.connection.getPacketContext());
        // The client played its own block's break sound, and particles of its model; a textured state uses this block's model
        boolean sound = clientState.getSoundType().getBreakSound() == state.getSoundType().getBreakSound();
        boolean particles = block instanceof StatePolymerBlock statePolymerBlock && statePolymerBlock.isTextured(state);
        return sound || particles ? new PredictedBreakEffects(serverPlayer, sound, particles) : null;
    }

    public static @Nullable ServerPlayer skippedBySound() {
        return CURRENT.isBound() && CURRENT.get().sound() ? CURRENT.get().player() : null;
    }

    public static boolean skipsParticles(ServerPlayer player) {
        return CURRENT.isBound() && CURRENT.get().particles() && CURRENT.get().player() == player;
    }

    // Polymer without this API can only tell creative, where the client always breaks the block itself
    private static boolean isBrokenByClient(ServerPlayer player, BlockPos pos) {
        if (IS_BROKEN_BY_CLIENT == null) {
            return player.getAbilities().instabuild;
        }
        try {
            return (boolean) IS_BROKEN_BY_CLIENT.invokeExact(player, pos);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static @Nullable MethodHandle findIsBrokenByClient() {
        try {
            return MethodHandles.publicLookup().findStatic(PolymerBlockUtils.class, "isBrokenByClient",
                MethodType.methodType(boolean.class, ServerPlayer.class, BlockPos.class));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
