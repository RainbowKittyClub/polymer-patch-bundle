package me.drex.ppb.block.type;

import com.brand.blockus.blocks.base.HedgeBlock;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record WallPolymerBlock<T extends Comparable<T>>(
    Property<T> north, Property<T> east,
    Property<T> south, Property<T> west,
    Function<T, Boolean> hasWall
) implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {

    public static final WallPolymerBlock<WallSide> BARRIER = new WallPolymerBlock<>(
        BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL,
        BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL,
        value -> value != WallSide.NONE
    );

    public static final WallPolymerBlock<Boolean> HEDGE = new WallPolymerBlock<>(
        HedgeBlock.NORTH, HedgeBlock.EAST,
        HedgeBlock.SOUTH, HedgeBlock.WEST,
        value -> value
    );

    private static final BlockState eastWest = PolymerBlockResourceUtils.requestEmpty(BlockModelType.GATE_EAST_WEST);
    private static final BlockState northSouth = PolymerBlockResourceUtils.requestEmpty(BlockModelType.GATE_NORTH_SOUTH);

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        Boolean north = blockState.getOptionalValue(north()).map(hasWall).orElse(false);
        Boolean east = blockState.getOptionalValue(east()).map(hasWall).orElse(false);
        Boolean south = blockState.getOptionalValue(south()).map(hasWall).orElse(false);
        Boolean west = blockState.getOptionalValue(west()).map(hasWall).orElse(false);
        if (east && west) {
            return northSouth;
        } else if (north && south) {
            return eastWest;
        }
        if (east || west) {
            return northSouth;
        } else if (north || south) {
            return eastWest;
        }

        return northSouth;
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
