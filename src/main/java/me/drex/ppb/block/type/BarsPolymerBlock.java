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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public record BarsPolymerBlock() implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {

    private static final Direction[] DIRECTIONS = new Direction[]{
        Direction.NORTH, Direction.EAST,
        Direction.SOUTH, Direction.WEST
    };
    private static final BooleanProperty[] DIRECTION_PROPERTIES = new BooleanProperty[]{
        BlockStateProperties.NORTH, BlockStateProperties.EAST,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST,
    };
    private static final BlockState[] EMPTY_STATES = new BlockState[32];

    public static final BarsPolymerBlock INSTANCE = new BarsPolymerBlock();

    static {
        for (boolean waterlogged : new boolean[]{false, true}) {
            int index = waterlogged ? 16 : 0;
            for (int i = 0; i < 16; i++) {
                var directions = new LinkedList<Direction>();
                for (int dir = 0; dir < 4; dir++) {
                    if ((i >> dir & 1) == 1) {
                        directions.add(DIRECTIONS[dir]);
                    }
                }
                EMPTY_STATES[index + i] = PolymerBlockResourceUtils.requestEmpty(BlockModelType.getBars(waterlogged, directions));
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        Boolean waterlogged = blockState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        int i = waterlogged ? 16 : 0;
        for (int dir = 0; dir < DIRECTION_PROPERTIES.length; dir++) {
            Boolean direction = blockState.getValueOrElse(DIRECTION_PROPERTIES[dir], false);
            if (direction) {
                i |= 1 << dir;
            }
        }

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
