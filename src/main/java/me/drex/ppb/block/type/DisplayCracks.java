package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.CustomBreakingParticleBlock;
import eu.pb4.factorytools.api.util.MiningStageModel;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.blocks.impl.BlockExtBlockMapper;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import me.drex.ppb.block.MiningCracks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Polymer block drawn by displays, whose mining cracks follow those displays' models instead of
 * factorytools' full cube, and whose hit particles the server sends when the client's stand-in has none.
 * A state with no display keeps only the client's own cracks, unless its stand-in draws none (a barrier).
 */
public interface DisplayCracks extends VirtualDestroyStage.Marker, CustomBreakingParticleBlock {
    @Override
    default boolean showCustomMiningStageMarker(BlockState state, BlockPos pos, ServerPlayer player) {
        return BlockBoundAttachment.get(player.level(), pos) != null
            || PolymerBlockUtils.getPolymerBlockState(state, player.connection.getPacketContext()).getRenderShape() == RenderShape.INVISIBLE;
    }

    @Override
    default @Nullable List<MiningStageModel> getMiningStageModels(BlockState state, BlockPos pos, ServerPlayer player) {
        return MiningCracks.Api.models(player.level(), pos);
    }

    // A barrier, or an empty carrier under a display; a textured carrier has no display
    @Override
    default boolean shouldSendHitParticles(BlockState state, BlockPos pos, ServerPlayer player) {
        BlockState clientState = PolymerBlockUtils.getPolymerBlockState(state, player.connection.getPacketContext());
        return clientState.getRenderShape() == RenderShape.INVISIBLE || !clientState.shouldSpawnTerrainParticles()
            || BlockExtBlockMapper.INSTANCE.stateMap.containsKey(clientState) && BlockBoundAttachment.get(player.level(), pos) != null;
    }
}
