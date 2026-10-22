package me.drex.ppb.block.type;

import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import me.drex.ppb.block.DetailedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block some players see as a chorus plant (same shape) under a display of the real block, and others as
 * a full-block stand-in. Cracks and hit dust follow what each player sees.
 */
public interface ChorusStandIn extends DisplayCracks {
    /**
     * Whether players with detailed small logs see this state as a chorus plant.
     */
    boolean drawsChorus(BlockState state);

    DetailedBlocks.Kind kind();

    default boolean seesChorus(BlockState state, ServerPlayer player) {
        return drawsChorus(state) && DetailedBlocks.shows(player, kind());
    }

    @Override
    default boolean showCustomMiningStageMarker(BlockState state, BlockPos pos, ServerPlayer player) {
        return seesChorus(state, player) && BlockBoundAttachment.get(player.level(), pos) != null;
    }

    // The client's own hit dust would come from the chorus plant
    @Override
    default boolean shouldSendHitParticles(BlockState state, BlockPos pos, ServerPlayer player) {
        return seesChorus(state, player);
    }

    // The stand-in at this position, when some players see it as a chorus plant
    static @Nullable ChorusStandIn at(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return PolymerSyncedObject.getSyncedObject(BuiltInRegistries.BLOCK, state.getBlock()) instanceof ChorusStandIn standIn
            && standIn.drawsChorus(state) ? standIn : null;
    }
}
