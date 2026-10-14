package me.drex.ppb.block.type;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.res.ResourceHelper;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public record StatePolymerBlock(Map<BlockState, BlockState> map,
                                FactoryBlock fallback) implements FactoryBlock, PolymerTexturedBlock, BSMMParticleBlock {

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type) {
        return of(id, block, type, BaseFactoryBlock.BARRIER);
    }

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type, FactoryBlock fallback) {
        return of(id, block, type, fallback, x -> true);
    }

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type, FactoryBlock fallback, Predicate<BlockState> canUseBlock) {
        try {
            BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);

            var list = new ArrayList<Pair<BlockStatePredicate, List<StateModelVariant>>>();
            var cache = new HashMap<List<StateModelVariant>, BlockState>();


            BlockStateModelManager.parseVariants(block, blockStateAsset.variants().orElseThrow(), (a, b) -> list.add(new Pair<>(a, b)));
            var map = new IdentityHashMap<BlockState, BlockState>();

            for (var state : block.getStateDefinition().getPossibleStates()) {
                for (var pair : list) {
                    if (pair.getFirst().test(state) && canUseBlock.test(state)) {
                        map.put(state, cache.computeIfAbsent(pair.getSecond(), c -> PolymerBlockResourceUtils.requestBlock(
                            type,
                            c.stream().map(x -> new PolymerBlockModel(x.model(), x.x(), x.y(), x.uvlock(), x.weigth())).toArray(PolymerBlockModel[]::new))));
                        break;
                    }
                }
            }
            PolymerPatchBundleMod.LOGGER.debug("{} uses block states {} from {}", id, cache.size(), type);
            return new StatePolymerBlock(map, fallback);
        } catch (Throwable e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to handle state block {}", id, e);
            return null;
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        var val = map.get(blockState);
        return val != null ? val : fallback.getPolymerBlockState(blockState, packetContext);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return map.get(initialBlockState) != null ? null : fallback.createElementHolder(world, pos, initialBlockState);
    }

    // True when the client gets a real textured state for this state, here or in a nested fallback.
    public boolean isTextured(BlockState blockState) {
        return map.get(blockState) != null || (fallback instanceof StatePolymerBlock statePolymerBlock && statePolymerBlock.isTextured(blockState));
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
