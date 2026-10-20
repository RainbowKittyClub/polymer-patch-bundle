package me.drex.ppb.res;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import me.drex.ppb.PolymerPatchBundleMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ResourceHelper {
    // The assets folder of every loaded mod, nested ones included, in mod id order
    private static final List<Path> MOD_ASSETS = FabricLoader.getInstance().getAllMods().stream()
        .filter(mod -> !mod.getMetadata().getType().equals("builtin"))
        .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
        .flatMap(mod -> mod.getRootPaths().stream())
        .map(root -> root.resolve("assets"))
        .filter(Files::isDirectory)
        .toList();
    // The root holding assets/: the client jar's on a server, the right game jar on a (split dev) client.
    private static final Path vanillaRoot = PolymerCommonUtils.getClientJarRoot();

    public static void init(ResourcePackBuilder packBuilder) {
        Set<String> added = new HashSet<>();
        for (Path assets : MOD_ASSETS) {
            for (String namespace : PolymerPatchBundleMod.MOD_ASSET_IDS) {
                Path namespaceRoot = assets.resolve(namespace);
                if (!Files.isDirectory(namespaceRoot)) continue;
                try (Stream<Path> files = Files.walk(namespaceRoot)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        String path = namespaceRoot.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
                        // Same as getAsset: the first mod with a file wins
                        if (!Identifier.isValidPath(path) || !added.add(namespace + ":" + path)) return;
                        try {
                            packBuilder.addData("assets/" + namespace + "/" + path, Files.readAllBytes(file));
                        } catch (IOException e) {
                            PolymerPatchBundleMod.LOGGER.error("Failed to read resource {}:{}: {}", namespace, path, e);
                        }
                    });
                } catch (IOException e) {
                    PolymerPatchBundleMod.LOGGER.error("Failed to list resources in {}: {}", namespaceRoot, e);
                }
            }
        }
    }

    public static IoSupplier<InputStream> getAsset(String namespace, String path) {
        for (Path assets : MOD_ASSETS) {
            Path file = assets.resolve(namespace).resolve(path);
            if (Files.isRegularFile(file)) {
                return IoSupplier.create(file);
            }
        }
        if (vanillaRoot == null) {
            return null;
        }
        var vanillaPath = vanillaRoot.resolve("assets/" + namespace + "/" + path);
        if (Files.exists(vanillaPath)) {
            return IoSupplier.create(vanillaPath);
        } else {
            return null;
        }
    }

    public static <T> T decodeAsset(Codec<T> codec, Identifier id, String type, String extension) throws IOException {
        IoSupplier<InputStream> supplier = getAsset(id.getNamespace(), type + "/" + id.getPath() + extension);
        return codec.decode(JsonOps.INSTANCE, JsonParser.parseReader(new JsonReader(new InputStreamReader(supplier.get())))).getOrThrow().getFirst();
    }

    public static BlockStateAsset decodeBlockState(Identifier id) throws IOException {
        return decodeAsset(BlockStateAsset.CODEC, id, "blockstates", ".json");
    }

    public static ModelAsset decodeModel(Identifier id) throws IOException {
        return decodeAsset(ModelAsset.CODEC, id, "models", ".json");
    }
}
