package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.block.BlockStateModel;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public record LanternPolymerBlock() implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {

    private static final BlockState[] EMPTY_STATES = new BlockState[4];

    public static final LanternPolymerBlock INSTANCE = new LanternPolymerBlock();

    static {
        int index = 0;
        for (boolean hanging : new boolean[]{false, true}) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                EMPTY_STATES[index] = PolymerBlockResourceUtils.requestEmpty(BlockModelType.getLantern(hanging, waterlogged));
                index++;
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        Boolean hanging = blockState.getValueOrElse(BlockStateProperties.HANGING, false);
        Boolean waterlogged = blockState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        int i = 0;
        i |= waterlogged ? 1 : 0;
        i |= hanging ? 2 : 0;

        return EMPTY_STATES[i];
    }

    @Override
    public @NotNull ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return BlockStateModel.midRange();
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
