package me.drex.ppb.block.type;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.block.Carriers;
import me.drex.ppb.block.PolymerBlockHelper;
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
                                FactoryBlock fallback) implements FactoryBlock, DisplayCracks, PolymerTexturedBlock, BSMMParticleBlock, TexturedStates {

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type) {
        return of(id, block, type, BaseFactoryBlock.BARRIER);
    }

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type, FactoryBlock fallback) {
        return of(id, block, type, fallback, x -> true);
    }

    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type, FactoryBlock fallback, Predicate<BlockState> canUseBlock) {
        return of(id, block, type, fallback, canUseBlock, null);
    }

    // As above, every stand-in drawn from the states standIn accepts (null: the pool's own default).
    public static StatePolymerBlock of(Identifier id, Block block, BlockModelType type, FactoryBlock fallback, Predicate<BlockState> canUseBlock, @Nullable Predicate<BlockState> standIn) {
        try {
            BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);

            var list = new ArrayList<Pair<BlockStatePredicate, List<StateModelVariant>>>();
            var cache = new HashMap<List<StateModelVariant>, BlockState>();


            BlockStateModelManager.parseVariants(block, blockStateAsset.variants().orElseThrow(), (a, b) -> list.add(new Pair<>(a, b)));
            var map = new IdentityHashMap<BlockState, BlockState>();

            // Stable slots first, so a state sharing its models with one never takes a normal state
            if (type == BlockModelType.FULL_BLOCK) {
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    var properties = Carriers.properties(state);
                    var variants = Carriers.holdsStableSlot(id, properties) ? match(list, state, canUseBlock) : null;
                    if (variants != null) {
                        cache.computeIfAbsent(variants, c -> request(type, id, properties, c, standIn));
                    }
                }
            }

            for (var state : block.getStateDefinition().getPossibleStates()) {
                var variants = match(list, state, canUseBlock);
                if (variants != null) {
                    map.put(state, cache.computeIfAbsent(variants, c -> request(type, id, Carriers.properties(state), c, standIn)));
                }
            }
            PolymerPatchBundleMod.LOGGER.debug("{} uses block states {} from {}", id, cache.size(), type);
            return new StatePolymerBlock(map, fallback);
        } catch (Throwable e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to handle state block {}", id, e);
            return null;
        }
    }

    private static @Nullable BlockState request(BlockModelType type, Identifier id, String properties, List<StateModelVariant> variants, @Nullable Predicate<BlockState> standIn) {
        return standIn == null
            ? PolymerBlockHelper.requestBlock(type, id, properties, variants)
            : PolymerBlockHelper.requestBlock(type, id, properties, variants, standIn);
    }

    // The models of the first variant matching this state, or null when it has none or can't use one
    private static @Nullable List<StateModelVariant> match(List<Pair<BlockStatePredicate, List<StateModelVariant>>> list, BlockState state, Predicate<BlockState> canUseBlock) {
        if (!canUseBlock.test(state)) {
            return null;
        }
        for (var pair : list) {
            if (pair.getFirst().test(state)) {
                return pair.getSecond();
            }
        }
        return null;
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
    @Override
    public boolean isTextured(BlockState blockState) {
        return map.get(blockState) != null || (fallback instanceof TexturedStates textured && textured.isTextured(blockState));
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
