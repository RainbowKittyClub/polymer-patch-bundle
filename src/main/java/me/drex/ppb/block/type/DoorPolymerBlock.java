package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.block.BlockStateModel;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

public record DoorPolymerBlock() implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {
    public static final DoorPolymerBlock INSTANCE = new DoorPolymerBlock();
    private static final Map<Direction, BlockState> STATES_REGULAR = Util.makeEnumMap(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf("TRAPDOOR_" + switch (x) {
        case UP -> "BOTTOM";
        case DOWN -> "TOP";
        default -> x.getSerializedName().toUpperCase(Locale.ROOT);
    })));

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return STATES_REGULAR.get(blockState.getValue(DoorBlock.OPEN)
                ? (blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? blockState.getValue(TrapDoorBlock.FACING).getCounterClockWise() : blockState.getValue(TrapDoorBlock.FACING).getClockWise())
                : blockState.getValue(TrapDoorBlock.FACING));
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
