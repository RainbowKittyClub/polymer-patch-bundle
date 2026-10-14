package me.drex.ppb.block;

import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.block.mod.*;
import me.drex.ppb.block.type.*;
import me.drex.ppb.mixin.BlockStateBaseAccessor;
import me.drex.ppb.res.ResourceHelper;
import me.drex.ppb.res.ResourcePackGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

public class PolymerBlockHelper {
    private static final Map<String, Supplier<ModPolymerBlockHelper>> MOD_HELPERS = Map.of(
        "blockus", () -> BlockusPolymerBlockHelper.INSTANCE,
        "traverse", () -> TraversePolymerBlockHelper.INSTANCE,
        "terrestria", () -> TerrestriaPolymerBlockHelper.INSTANCE,
        "cinderscapes", () -> CinderscapesPolymerBlockHelper.INSTANCE,
        "woods_and_mires", () -> WoodsAndMiresPolymerBlockHelper.INSTANCE
    );

    public static void registerPolymerBlock(Identifier id, Block block) {
        boolean transparent = block instanceof TransparentBlock || block instanceof StainedGlassPaneBlock;

        if (transparent) {
            BlockStateModelManager.addBlock(id, block);
        } else {
            BlockStateModelManager.addBlock(id, block);
        }

        PolymerBlock polymerBlock = requestPolymerBlock(id, block);
        disableDisplayOcclusion(block, polymerBlock);
        PolymerBlock.registerOverlay(block, polymerBlock);
        if (polymerBlock instanceof BlockWithElementHolder blockWithElementHolder) {
            BlockWithElementHolder.registerOverlay(block, blockWithElementHolder);
        }
    }

    // Displays light themselves from inside their block, so display-rendered states must not occlude;
    // textured states keep the block's own occlusion. Runs before Fabric's registry callback builds
    // the state caches (initCache), so light and shapes follow it.
    private static void disableDisplayOcclusion(Block block, PolymerBlock polymerBlock) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (!(polymerBlock instanceof StatePolymerBlock statePolymerBlock && statePolymerBlock.isTextured(state))) {
                ((BlockStateBaseAccessor) state).ppb$setCanOcclude(false);
            }
        }
    }

    public static PolymerBlock requestPolymerBlock(Identifier id, Block block) {
        Supplier<ModPolymerBlockHelper> modPolymerBlockHelper = MOD_HELPERS.get(id.getNamespace());
        if (modPolymerBlockHelper != null) {
            PolymerBlock polymerBlock = modPolymerBlockHelper.get().requestPolymerBlock(id, block);
            if (polymerBlock != null) {
                return polymerBlock;
            }
        }

        var polymerBlock = switch (block) {
            case WallBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.WALL);
            case StairBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.STAIR);
            case FenceBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.FENCE);
            case FenceGateBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.FENCE_GATE);
            case ButtonBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.BUTTON);
            case BasePressurePlateBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.PRESSURE_PLATE);
            case CarpetBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.CARPET);
            case ShelfBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.SHELF);
            case WallSignBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.WALL_SIGN);
            case WallHangingSignBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.HANGING_WALL_SIGN);
            case CeilingHangingSignBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.HANGING_SIGN);
            case StandingSignBlock ignored -> ResourcePackGenerator.expandBlockModel(id, StateCopyFactoryBlock.SIGN);
            case DoorBlock ignored -> DoorPolymerBlock.INSTANCE;
            case IronBarsBlock ignored -> BarsPolymerBlock.INSTANCE;
            case ChainBlock ignored -> ChainPolymerBlock.INSTANCE;
            case LanternBlock ignored -> LanternPolymerBlock.INSTANCE;
            case TrapDoorBlock ignored -> TrapdoorPolymerBlock.INSTANCE;
            case BedBlock ignored -> BedPolymerBlock.INSTANCE;
            case SlabBlock ignored -> SlabPolymerBlock.of(id);
            case FlowerPotBlock ignored -> ResourcePackGenerator.expandBlockModel(id, BaseFactoryBlock.POT);
            case WaterloggedTransparentBlock ignored -> BaseFactoryBlock.BARRIER;
            case LeavesBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.BIOME_COLORED_LEAVES);
//            case SnowyDirtBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case ColoredFallingBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
            case FarmlandBlock ignored -> StatePolymerBlock.of(id, block, BlockModelType.FARMLAND);
            case VegetationBlock ignored -> BaseFactoryBlock.PLANT;
            default -> {
                if (block.getClass().equals(Block.class)) {
                    // TODO log debug
                    yield StatePolymerBlock.of(id, block, BlockModelType.FULL_BLOCK);
                } else {
                    PolymerPatchBundleMod.LOGGER.debug("Missing overlay for block: '{}' {}", id, block.getClass().getName());
                    yield null;
                }
            }
        };
        if (polymerBlock == null) {
            if (block.defaultBlockState().getCollisionShape(PolymerCommonUtils.getFakeWorld(), BlockPos.ZERO).isEmpty()) {
                polymerBlock = BaseFactoryBlock.PLANT;
            } else {
                polymerBlock = BaseFactoryBlock.BARRIER;
            }
        }
        return polymerBlock;
    }

    public static BlockState requestPolymerBlockState(Identifier id, String variant, BlockModelType blockModelType) throws IOException {
        BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);

        var model = blockStateAsset.variants().get().get(variant);

        return PolymerBlockResourceUtils.requestBlock(
            blockModelType,
            model.stream().map(x -> new PolymerBlockModel(x.model(), x.x(), x.y(), x.uvlock(), x.weigth())).toArray(PolymerBlockModel[]::new));
    }
}
