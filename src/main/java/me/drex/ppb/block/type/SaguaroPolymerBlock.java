package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.block.DetailedBlocks;
import me.drex.ppb.block.SmallLogDisplays;
import me.drex.ppb.block.SmallLogModel;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A saguaro cactus, shaped like a small log: a chorus plant under a display of the real cactus for players
 * with detailed small logs, else a plain cactus.
 */
public record SaguaroPolymerBlock(Identifier id) implements FactoryBlock, ChorusStandIn, PolymerTexturedBlock, BSMMParticleBlock {
    public static PolymerBlock of(Identifier id) {
        return SmallLogDisplays.register(id) ? new SaguaroPolymerBlock(id) : BaseFactoryBlock.CACTUS;
    }

    @Override
    public boolean drawsChorus(BlockState state) {
        return true;
    }

    @Override
    public DetailedBlocks.Kind kind() {
        return DetailedBlocks.Kind.SAGUARO;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return DetailedBlocks.shows(context, DetailedBlocks.Kind.SAGUARO) ? Blocks.CHORUS_PLANT.withPropertiesOf(state) : Blocks.CACTUS.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new SmallLogModel(this.id, DetailedBlocks.Kind.SAGUARO);
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
