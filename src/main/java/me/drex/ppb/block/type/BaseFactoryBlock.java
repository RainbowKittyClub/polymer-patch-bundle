package me.drex.ppb.block.type;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public record BaseFactoryBlock(Function<BlockState, BlockState> blockStateFunction, boolean tick,
                               BiFunction<BlockState, BlockPos, BlockModel> modelFunction) implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock {
    public BaseFactoryBlock(BlockState clientState, boolean tick,
                            BiFunction<BlockState, BlockPos, BlockModel> modelFunction) {
        this(state -> clientState, tick, modelFunction);
    }

    public static final BaseFactoryBlock BARRIER = new BaseFactoryBlock(blockState -> Blocks.BARRIER.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, blockState.getValueOrElse(BlockStateProperties.WATERLOGGED, false)), false, (state, blockPos) -> BlockStateModel.longRange());
    public static final BaseFactoryBlock POT = new BaseFactoryBlock(Blocks.FLOWER_POT.defaultBlockState(), false, (state, blockPos) -> BlockStateModel.midRange());
    public static final BaseFactoryBlock PLANT = new BaseFactoryBlock(PolymerBlockResourceUtils.requestEmpty(BlockModelType.PLANT), false, (state, blockPos) -> BlockStateModel.midRange());
    public static final BaseFactoryBlock CACTUS = new BaseFactoryBlock(Blocks.CACTUS.defaultBlockState(), false, (blockState, blockPos) -> null);

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return blockStateFunction.apply(state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return this.modelFunction.apply(initialBlockState, pos);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return this.tick;
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
