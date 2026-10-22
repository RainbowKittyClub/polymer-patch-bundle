package me.drex.ppb.block;

import eu.pb4.factorytools.api.util.LazyItemStack;
import eu.pb4.factorytools.api.util.MiningStageModel;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.res.ResourceHelper;
import me.drex.ppb.res.ResourcePackGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Crack models shaped like the displays that draw a block, for factorytools' mining stage marker.
 * A factorytools without that API keeps its full cube; nothing here touches it then.
 */
public final class MiningCracks {
    public static final boolean SUPPORTED = hasApi();
    private static final Set<Identifier> DISPLAY_BLOCKS = new TreeSet<>();

    private MiningCracks() {
    }

    public static void setup() {
        if (SUPPORTED) {
            PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> Api.request());
        }
    }

    // A block whose element holder draws it, so its models get crack models
    public static void register(Identifier blockId) {
        if (SUPPORTED) {
            synchronized (DISPLAY_BLOCKS) {
                DISPLAY_BLOCKS.add(blockId);
            }
        }
    }

    private static boolean hasApi() {
        try {
            Class.forName("eu.pb4.factorytools.api.util.MiningStageModel", false, MiningCracks.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static final class Api {
        // Display model -> its crack models, filled when the pack is built
        private static volatile @Nullable Map<Identifier, List<LazyItemStack>> stages;

        private Api() {
        }

        static synchronized void request() {
            if (stages != null) {
                return;
            }
            List<Identifier> blocks;
            synchronized (DISPLAY_BLOCKS) {
                blocks = List.copyOf(DISPLAY_BLOCKS);
            }
            var map = new HashMap<Identifier, List<LazyItemStack>>();
            for (var block : blocks) {
                for (var model : displayModels(block)) {
                    if (map.containsKey(model)) continue;
                    // One bad model keeps the cube for its block, not the whole pack build from finishing
                    try {
                        map.put(model, MiningStageModel.stagesOf(ResourcePackGenerator.servedGeometry(model)));
                    } catch (Throwable e) {
                        PolymerPatchBundleMod.LOGGER.warn("Failed to request crack models for {}: {}", model, e);
                    }
                }
            }
            stages = Map.copyOf(map);
        }

        // One crack model per display of the block, each with that display's transformation; null keeps the cube
        public static @Nullable List<MiningStageModel> models(Level level, BlockPos pos) {
            var stages = Api.stages;
            var attachment = BlockBoundAttachment.get(level, pos);
            if (stages == null || attachment == null || !(attachment.holder() instanceof BlockStateModel model)) {
                return null;
            }

            var holderOffset = attachment.getPos().subtract(Vec3.atCenterOf(pos));
            var list = new ArrayList<MiningStageModel>();
            for (var element : model.modelElements()) {
                var itemModel = element.getItem().get(DataComponents.ITEM_MODEL);
                if (itemModel == null) {
                    continue;
                }
                var crack = stages.get(itemModel.withPath(path -> path.substring("-/".length())));
                if (crack == null) {
                    return null;
                }
                list.add(new MiningStageModel(crack, transformation(element, holderOffset)));
            }
            return list.isEmpty() ? null : list;
        }

        // The display's own model space, as the client places it: position, entity rotation, then transformation
        private static Matrix4f transformation(ItemDisplayElement element, Vec3 holderOffset) {
            var offset = holderOffset.add(element.getOffset());
            return new Matrix4f()
                .translate((float) offset.x, (float) offset.y, (float) offset.z)
                .rotateYXZ(-element.getYaw() * Mth.DEG_TO_RAD, element.getPitch() * Mth.DEG_TO_RAD, 0)
                .translate(element.getTranslation())
                .rotate(element.getLeftRotation())
                .scale(element.getScale())
                .rotate(element.getRightRotation());
        }

        private static List<Identifier> displayModels(Identifier block) {
            BlockStateAsset asset;
            try {
                asset = ResourceHelper.decodeBlockState(block);
            } catch (Throwable e) {
                PolymerPatchBundleMod.LOGGER.warn("Failed to read blockstate {} for its crack models: {}", block, e);
                return List.of();
            }
            var models = new ArrayList<Identifier>();
            asset.variants().ifPresent(variants -> variants.values().forEach(list -> list.forEach(v -> models.add(displayModel(v)))));
            asset.multipart().ifPresent(parts -> parts.forEach(part -> part.apply().forEach(v -> models.add(displayModel(v)))));
            return models;
        }

        // As BlockStateModelManager picks it
        private static Identifier displayModel(StateModelVariant variant) {
            return variant.uvlock() ? variant.model().withSuffix("_uvlock_" + variant.x() + "_" + variant.y()) : variant.model();
        }
    }
}
