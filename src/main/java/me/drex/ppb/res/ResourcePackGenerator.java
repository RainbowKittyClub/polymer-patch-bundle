package me.drex.ppb.res;


import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.resourcepack.ModelModifiers;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateMultiPartDefinition;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelElement;
import me.drex.ppb.PolymerPatchBundleMod;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

import static me.drex.ppb.PolymerPatchBundleMod.id;

public class ResourcePackGenerator {
    private static final Vec3 EXPANSION = new Vec3(0.08, 0.08, 0.08);
    public static final Set<Identifier> EXPANDABLE_MODELS = new HashSet<>();

    public static void setup() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(ResourcePackGenerator::build);
    }

    public static PolymerBlock expandBlockModel(Identifier id, PolymerBlock polymerBlock) {
        return expandBlockModel(id, polymerBlock, x -> true);
    }

    public static PolymerBlock expandBlockModel(Identifier id, PolymerBlock polymerBlock, Predicate<String> variantPredicate) {
        try {
            BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);

            Map<String, List<StateModelVariant>> variants = blockStateAsset.variants().orElse(Collections.emptyMap());
            variants.entrySet().forEach(entry -> {
                if (variantPredicate.test(entry.getKey())) {
                    entry.getValue().forEach(variant -> expandModel(variant.model()));
                }
            });

            List<StateMultiPartDefinition> multiParts = blockStateAsset.multipart().orElse(Collections.emptyList());
            multiParts.forEach(stateMultiPartDefinition -> stateMultiPartDefinition.apply().forEach(stateModelVariant -> expandModel(stateModelVariant.model())));
        } catch (Throwable e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to read blockstate {}: {}", id, e);
        }
        return polymerBlock;
    }

    private static void expandModel(Identifier id) {
        try {
            EXPANDABLE_MODELS.add(id.withSuffix(".json"));
            ModelAsset modelAsset = ResourceHelper.decodeModel(id);
            modelAsset.parent().ifPresent(ResourcePackGenerator::expandModel);
        } catch (Throwable e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to read model {}: {}", id, e);
        }
    }

    private static void build(ResourcePackBuilder builder) {
        var atlas = AtlasAsset.builder();
        // forEachResource walks a Map.copyOf, whose order changes per run, and a template is both
        // expanded itself and read back as a child's parent; writing afterwards expands it once.
        Map<String, byte[]> expanded = new TreeMap<>();
        builder.forEachResource((string, packResource) -> {
            String[] parts = string.split("/", 4);
            if (parts.length < 4) return;
            try {
                Identifier id = Identifier.fromNamespaceAndPath(parts[1], parts[3]);
                if (!parts[0].equals("assets") || !parts[2].equals("models")) return;
                if (!EXPANDABLE_MODELS.contains(id)) return;
                var asset = ModelAsset.fromJson(new String(packResource.readAllBytes(), StandardCharsets.UTF_8));
                if (asset.parent().isPresent()) {
                    var parentId = asset.parent().get();
                    var parentAsset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getDataOrSource(AssetPaths.model(parentId) + ".json")), StandardCharsets.UTF_8));

                    expanded.put(AssetPaths.model(PolymerPatchBundleMod.MOD_ID, parentId.getPath()) + ".json", new ModelAsset(parentAsset.parent(), parentAsset.elements().map(x -> x.stream()
                        .map(element -> new ModelElement(element.from().subtract(EXPANSION), element.to().add(EXPANSION),
                            element.faces(), element.rotation(), element.shade(), element.lightEmission())
                        ).toList()), parentAsset.textures(), parentAsset.display(), parentAsset.guiLight(), parentAsset.ambientOcclusion()).toBytes());
                    if (asset.elements().isPresent()) {
                        expanded.put(string, new ModelAsset(asset.parent(), asset.elements().map(x -> x.stream()
                            .map(element -> new ModelElement(element.from().subtract(EXPANSION), element.to().add(EXPANSION),
                                element.faces(), element.rotation(), element.shade(), element.lightEmission())
                            ).toList()), asset.textures(), asset.display(), asset.guiLight(), asset.ambientOcclusion()).toBytes());
                    }
                }
            } catch (IdentifierException ignored) {
            }
        });
        expanded.forEach(builder::addData);

        PolymerPatchBundleMod.MOD_ASSET_IDS.forEach(modid -> {
            for (var entry : BlockStateModelManager.UV_LOCKED_MODELS.getOrDefault(modid, Collections.emptyMap()).entrySet()) {
                String path = entry.getKey();
                Identifier id = Identifier.fromNamespaceAndPath(modid, path).withSuffix(".json");

                var expand = EXPANDABLE_MODELS.contains(id) ? EXPANSION : Vec3.ZERO;

                for (var v : entry.getValue()) {
                    var suffix = "_uvlock_" + v.x() + "_" + v.y();
                    var modelId = v.model().withSuffix(suffix);
                    var asset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getData(AssetPaths.model(v.model()) + ".json")), StandardCharsets.UTF_8));

                    if (asset.parent().isPresent()) {
                        var parentId = asset.parent().get();
                        var parentAsset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getDataOrSource(AssetPaths.model(parentId) + ".json")), StandardCharsets.UTF_8));
                        builder.addData(AssetPaths.model(PolymerPatchBundleMod.MOD_ID, parentId.getPath() + suffix) + ".json",
                            ModelModifiers.expandModelAndRotateUVLocked(parentAsset, expand, v.x(), v.y()));
                        builder.addData(AssetPaths.model(modelId) + ".json",
                            new ModelAsset(Optional.of(Identifier.fromNamespaceAndPath(PolymerPatchBundleMod.MOD_ID, parentId.getPath() + suffix)), asset.elements(),
                                asset.textures(), asset.display(), asset.guiLight(), asset.ambientOcclusion()).toBytes());
                    }
                }
            }
        });

        builder.addResourceConverter((string, resource) -> {
            if (!string.contains("_uvlock_")) {
                String[] parts = string.split("/", 4);
                if (parts.length < 4) return resource;
                try {
                    Identifier id = Identifier.fromNamespaceAndPath(parts[1], parts[3]);
                    if (!parts[0].equals("assets") || !parts[2].equals("models")) return resource;
                    if (!EXPANDABLE_MODELS.contains(id)) return resource;

                    var asset = ModelAsset.fromJson(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
                    return PackResource.of(new ModelAsset(asset.parent().map(x -> id(x.getPath())), asset.elements(), asset.textures(), asset.display(), asset.guiLight(), asset.ambientOcclusion()).toBytes());
                } catch (IdentifierException ignored) {
                }
            }
            return resource;
        });

        builder.addData("assets/minecraft/atlases/blocks.json", atlas.build());
    }
}