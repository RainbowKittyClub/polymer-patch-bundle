package me.drex.ppb.block.mod;

import com.terraformersmc.terraform.dirt.api.block.TerraformDirtPathBlock;
import com.terraformersmc.terraform.leaves.api.block.LeafPileBlock;
import com.terraformersmc.terraform.wood.api.block.QuarterLogBlock;
import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import com.terraformersmc.terrestria.Terrestria;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import me.drex.ppb.block.type.BaseFactoryBlock;
import me.drex.ppb.block.type.SmallLogPolymerBlock;
import me.drex.ppb.block.type.StatePolymerBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import org.jetbrains.annotations.Nullable;

public class TerraformPolymerBlockHelper implements ModPolymerBlockHelper {
    public static final TerraformPolymerBlockHelper INSTANCE = new TerraformPolymerBlockHelper();

    @Override
    public @Nullable PolymerBlock requestPolymerBlock(Identifier id, Block block) {
        return switch (block) {
            case SmallLogBlock smallLog -> SmallLogPolymerBlock.of(id, smallLog);
            case QuarterLogBlock ignored -> {
                if (Terrestria.getConfigManager().getGeneralConfig().areQuarterLogsEnabled()) {
                    yield StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
                }
                yield BaseFactoryBlock.BARRIER;
            }
            case RotatedPillarBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case TerraformDirtPathBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FARMLAND);
            case LeafPileBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.TRIPWIRE_FLAT);
            default -> null;
        };
    }
}
