package me.drex.ppb.block.mod;

import com.terraformersmc.cinderscapes.block.*;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import me.drex.ppb.block.type.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class CinderscapesPolymerBlockHelper implements ModPolymerBlockHelper {
    public static final CinderscapesPolymerBlockHelper INSTANCE = new CinderscapesPolymerBlockHelper();
    // Ground cover players walk through: a vine is climbable on the client, which caps walking speed.
    private static final Set<String> CARPET_PLANTS = Set.of("scorched_sprouts", "scorched_tendrils", "scorched_shrub",
        "twilight_fescues", "twilight_tendrils");

    @Override
    public @Nullable PolymerBlock requestPolymerBlock(Identifier id, Block block) {
        PolymerBlock polymerBlock = TerraformPolymerBlockHelper.INSTANCE.requestPolymerBlock(id, block);
        if (polymerBlock != null) return polymerBlock;

        return switch (block) {
            // This is not great, but everything else seems worse
            // Could be improved by using textured blockstates for layers 1 and 8, but models for 2-7
            // but the holders should only be created when necessary
            case AshLayerBlock ignored -> LayerPolymerBlock.of(id, block);
            case VegetationBlock ignored when CARPET_PLANTS.contains(id.getPath()) ->
                StatePolymerBlock.of(id, block, BlockModelType.TRIPWIRE, BaseFactoryBlock.PLANT);
            // A vine stand-in has a column outline and no sideways connections, but players can climb it.
            case VegetationBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.VINES, BaseFactoryBlock.PLANT);
            case GhastlyEctoplasmBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.VINES, BaseFactoryBlock.PLANT);
            case PolypiteQuartzBlock ignored -> PolypiteQuartzBlockPolymerBlock.textured(id, block);
            case CinderscapesTransparentBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case CinderscapesNyliumBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case CinderscapesOreBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case LeavesBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            default -> null;
        };
    }
}
