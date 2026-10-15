package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.block.BlockStateModel;
import me.drex.ppb.block.PolymerBlockHelper;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;

public record SlabPolymerBlock(BlockState bottomState,
                               BlockState bottomStateWaterlogged) implements FactoryBlock, PolymerTexturedBlock, BSMMParticleBlock, TexturedStates {

    private static final BlockState[] EMPTY_STATES = new BlockState[4];

    static {
        int i = 0;
        for (boolean bottom : new boolean[]{false, true}) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                EMPTY_STATES[i] = PolymerBlockResourceUtils.requestEmpty(BlockModelType.getSlab(bottom, waterlogged));
                i++;
            }
        }
    }

    public static SlabPolymerBlock of(Identifier id) {
        try {
            BlockState bottom = PolymerBlockHelper.requestPolymerBlockState(id, "type=bottom", BlockModelType.SCULK_SENSOR);
            BlockState bottomWaterlogged = PolymerBlockHelper.requestPolymerBlockState(id, "type=bottom", BlockModelType.SCULK_SENSOR_WATERLOGGED);
            return new SlabPolymerBlock(bottom, bottomWaterlogged);
        } catch (IOException e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to handle slab block {}", id, e);
            return null;
        }
    }

    @Override
    public boolean isTextured(BlockState state) {
        return state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
            && (state.getValue(SlabBlock.WATERLOGGED) ? bottomStateWaterlogged : bottomState) != null;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        SlabType slabType = state.getValue(SlabBlock.TYPE);
        Boolean waterlogged = state.getValue(SlabBlock.WATERLOGGED);
        if (slabType == SlabType.BOTTOM) {
            var vanillaState = waterlogged ? bottomStateWaterlogged : bottomState;
            if (vanillaState != null) {
                return vanillaState;
            }
        }

        if (slabType == SlabType.DOUBLE) {
            return Blocks.BARRIER.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        } else {
            int i = slabType == SlabType.TOP ? 0 : 2;
            if (waterlogged) i++;
            return EMPTY_STATES[i];
        }
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        SlabType slabType = initialBlockState.getValue(SlabBlock.TYPE);
        var vanillaState = initialBlockState.getValue(SlabBlock.WATERLOGGED) ? bottomStateWaterlogged : bottomState;
        if (slabType == SlabType.BOTTOM && vanillaState != null) {
            return null;
        }
        return BlockStateModel.midRange();
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
