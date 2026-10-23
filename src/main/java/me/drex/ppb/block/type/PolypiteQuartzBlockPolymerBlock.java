package me.drex.ppb.block.type;

import com.terraformersmc.cinderscapes.block.PolypiteQuartzBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.block.BlockStateModel;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record PolypiteQuartzBlockPolymerBlock() implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {
    public static final PolypiteQuartzBlockPolymerBlock INSTANCE = new PolypiteQuartzBlockPolymerBlock();
    private static final Map<Direction, BlockState> STATES_REGULAR = Util.makeEnumMap(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.getTrapdoor(x, false)));

    // Each facing draws from its own trapdoor pool, so the cluster needs no display; a state its pool
    // can't take keeps the empty trapdoor and a display.
    public static FactoryBlock textured(Identifier id, Block block) {
        FactoryBlock result = INSTANCE;
        for (Direction direction : Direction.values()) {
            var facing = StatePolymerBlock.of(id, block, BlockModelType.getTrapdoor(direction.getOpposite(), false), result,
                state -> state.getValue(PolypiteQuartzBlock.DIRECTION) == direction);
            if (facing != null) {
                result = facing;
            }
        }
        return result;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        Direction direction = blockState.getValue(PolypiteQuartzBlock.DIRECTION);
        return STATES_REGULAR.get(direction.getOpposite());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return BlockStateModel.midRange();
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
