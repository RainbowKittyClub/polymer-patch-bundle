package me.drex.ppb.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateMultiPartDefinition;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.res.ResourceHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Draws a Terraform small log (or saguaro) of any arm mask with one item display: per block, a composite item
 * model of its six blockstate parts, each showing the arm or its stub by one custom model data flag and
 * rotated the way the blockstate rotates it.
 */
public final class SmallLogDisplays {
    // The order of the parts, and of their flags
    public static final List<Direction> ARMS = List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final Map<Identifier, List<Part>> BLOCKS = new TreeMap<>();

    private SmallLogDisplays() {
    }

    /**
     * One blockstate part: the model drawn when the arm is there, the one drawn when it isn't, and their
     * rotation.
     */
    public record Part(Identifier arm, Identifier stub, int x, int y) {
        // The blockstate's own turn (x, then y, both negative), for use inside the item model
        public Quaternionf modelRotation() {
            return new Quaternionf().rotateY(-Mth.DEG_TO_RAD * y).rotateX(-Mth.DEG_TO_RAD * x);
        }

        // The same turn as a display's own rotation, which the item display's extra 180° turn mirrors back
        public Quaternionf displayRotation() {
            return new Quaternionf().rotateY(-Mth.DEG_TO_RAD * y).rotateX(Mth.DEG_TO_RAD * x);
        }
    }

    public static void setup() {
        // A chorus arm's end face lies on the block edge, where a small log's branch ends too; on real chorus it
        // only ever sits inside a joint, so dropping it changes nothing there
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> {
            String path = "assets/minecraft/models/block/chorus_plant_side.json";
            byte[] vanilla = builder.getDataOrSource(path);
            if (vanilla == null) {
                PolymerPatchBundleMod.LOGGER.warn("Couldn't read {}; chorus arm ends may z-fight with small logs", path);
                return;
            }
            JsonObject model = JsonParser.parseString(new String(vanilla, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : model.getAsJsonArray("elements")) {
                element.getAsJsonObject().getAsJsonObject("faces").remove("north");
            }
            builder.addData(path, model.toString().getBytes(StandardCharsets.UTF_8));
        });
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            // Chorus's three nubbed side variants reach the log's surface or past it; the plain one stays 1 px
            // inside, so the stand-in never shows. Real chorus plants lose their nubs for pack clients
            for (String nubbed : List.of("chorus_plant_noside1", "chorus_plant_noside2", "chorus_plant_noside3")) {
                builder.addData("assets/minecraft/models/block/" + nubbed + ".json",
                    "{\"parent\":\"minecraft:block/chorus_plant_noside\"}".getBytes(StandardCharsets.UTF_8));
            }
            synchronized (BLOCKS) {
                BLOCKS.forEach((id, parts) -> builder.addData(
                    "assets/" + PolymerPatchBundleMod.MOD_ID + "/items/" + itemModel(id).getPath() + ".json",
                    itemDefinition(parts).toString().getBytes(StandardCharsets.UTF_8)));
            }
        });
    }

    /**
     * Reads the block's six parts from its blockstate. Returns false, and draws nothing, when the blockstate
     * isn't six arm/stub pairs. The models stay ungrown: their tiles share planes, so growing them overlaps.
     */
    public static boolean register(Identifier blockId) {
        List<Part> parts = readParts(blockId);
        if (parts == null) {
            PolymerPatchBundleMod.LOGGER.warn("Can't read the six arm parts of {}; it keeps its full-block stand-in", blockId);
            return false;
        }
        synchronized (BLOCKS) {
            BLOCKS.put(blockId, parts);
        }
        return true;
    }

    public static List<Part> parts(Identifier blockId) {
        synchronized (BLOCKS) {
            return BLOCKS.getOrDefault(blockId, List.of());
        }
    }

    public static ItemStack stack(Identifier blockId, BlockState state) {
        List<Boolean> flags = new ArrayList<>(ARMS.size());
        for (Direction arm : ARMS) {
            flags.add(state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(arm)));
        }
        ItemStack stack = new ItemStack(Items.TRIAL_KEY);
        stack.set(DataComponents.ITEM_MODEL, itemModel(blockId));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), flags, List.of(), List.of()));
        return stack;
    }

    private static Identifier itemModel(Identifier blockId) {
        return Identifier.fromNamespaceAndPath(PolymerPatchBundleMod.MOD_ID, "small_logs/" + blockId.getNamespace() + "/" + blockId.getPath());
    }

    private static @Nullable List<Part> readParts(Identifier blockId) {
        BlockStateAsset asset;
        try {
            asset = ResourceHelper.decodeBlockState(blockId);
        } catch (Throwable e) {
            return null;
        }
        List<Part> parts = new ArrayList<>(ARMS.size());
        for (Direction arm : ARMS) {
            StateModelVariant present = partWhen(asset, arm, "true");
            StateModelVariant absent = partWhen(asset, arm, "false");
            if (present == null || absent == null || present.x() != absent.x() || present.y() != absent.y()) {
                return null;
            }
            parts.add(new Part(present.model(), absent.model(), present.x(), present.y()));
        }
        return parts;
    }

    // The part applied on exactly {arm: value}, ignoring the leaves parts that also test has_leaves
    private static @Nullable StateModelVariant partWhen(BlockStateAsset asset, Direction arm, String value) {
        for (StateMultiPartDefinition part : asset.multipart().orElse(List.of())) {
            if (part.when().orElse(null) instanceof StateMultiPartDefinition.KeyValueCondition condition
                && condition.tests().size() == 1
                && condition.tests().get(arm.getSerializedName()) instanceof StateMultiPartDefinition.KeyValueCondition.Terms terms
                && terms.toString().equals(value) && !part.apply().isEmpty()) {
                return part.apply().getFirst();
            }
        }
        return null;
    }

    private static JsonObject itemDefinition(List<Part> parts) {
        JsonArray models = new JsonArray();
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            JsonObject condition = new JsonObject();
            condition.addProperty("type", "minecraft:condition");
            condition.addProperty("property", "minecraft:custom_model_data");
            condition.addProperty("index", i);
            condition.add("on_true", model(part.arm(), part.modelRotation()));
            condition.add("on_false", model(part.stub(), part.modelRotation()));
            models.add(condition);
        }
        JsonObject composite = new JsonObject();
        composite.addProperty("type", "minecraft:composite");
        composite.add("models", models);
        JsonObject root = new JsonObject();
        root.add("model", composite);
        return root;
    }

    // The item transformation applies to raw 0..1 model coordinates, so the rotation is wrapped to turn
    // about the block centre, as a display's own rotation does
    private static JsonObject model(Identifier model, Quaternionf rotation) {
        Vector3f translation = new Vector3f(0.5F).sub(rotation.transform(new Vector3f(0.5F)));
        JsonObject transformation = new JsonObject();
        transformation.add("translation", array(round(translation.x), round(translation.y), round(translation.z)));
        transformation.add("left_rotation", array(round(rotation.x), round(rotation.y), round(rotation.z), round(rotation.w)));
        transformation.add("scale", array(1, 1, 1));
        transformation.add("right_rotation", array(0, 0, 0, 1));
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:model");
        json.addProperty("model", model.toString());
        json.add("transformation", transformation);
        return json;
    }

    private static float round(float value) {
        return Math.round(value * 1e6F) / 1e6F;
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }
}
