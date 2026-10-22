package me.drex.ppb.block.mod;

import com.terraformersmc.terrestria.block.BasaltGrassBlock;
import com.terraformersmc.terrestria.block.CattailBlock;
import com.terraformersmc.terrestria.block.SaguaroCactusBlock;
import com.terraformersmc.terrestria.block.TallCattailBlock;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import me.drex.ppb.block.type.BaseFactoryBlock;
import me.drex.ppb.block.type.SaguaroPolymerBlock;
import me.drex.ppb.block.type.StatePolymerBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TerrestriaPolymerBlockHelper implements ModPolymerBlockHelper {
    public static final TerrestriaPolymerBlockHelper INSTANCE = new TerrestriaPolymerBlockHelper();
    // Leaves whose textures are already coloured; a foliage-tinted carrier turns them green
    private static final Set<String> UNTINTED_LEAVES = Set.of("sakura_leaves", "yucca_palm_leaves",
        "japanese_maple_leaves", "dark_japanese_maple_leaves", "jungle_palm_leaves");

    @Override
    public @Nullable PolymerBlock requestPolymerBlock(Identifier id, Block block) {
        PolymerBlock polymerBlock = TerraformPolymerBlockHelper.INSTANCE.requestPolymerBlock(id, block);
        if (polymerBlock != null) return polymerBlock;

        if (UNTINTED_LEAVES.contains(id.getPath())) {
            return StatePolymerBlock.of(id, block, BlockModelType.LEAVES);
        }
        return switch (block) {
            case SaguaroCactusBlock ignored -> SaguaroPolymerBlock.of(id);
            case BasaltGrassBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.BIOME_COLORED_LEAVES, StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK), state -> !state.getValue(BlockStateProperties.SNOWY));
            case CattailBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.KELP);
            // Clients fill kelp with water, and the upper half stands above it
            case TallCattailBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.KELP,
                StatePolymerBlock.of(id, block, BlockModelType.PLANT, BaseFactoryBlock.PLANT, TerrestriaPolymerBlockHelper::isUpperHalf),
                state -> !isUpperHalf(state));
            default -> null;
        };
    }

    private static boolean isUpperHalf(BlockState state) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
    }
}
