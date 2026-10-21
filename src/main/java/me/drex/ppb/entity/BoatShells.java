package me.drex.ppb.entity;

import com.google.gson.JsonObject;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.res.ResourceHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Textured shells over modded boats for pack clients: an item model of the vanilla hull, held by a
// carrier dolphin riding last (ShellCarrier), drawn exactly over the nearest-wood stand-in boat
public final class BoatShells {
    // Carrier scales: far it stops drawing at the same distance as the boat, near its box stays small
    public static final float FAR_SCALE = 1.37f;
    public static final float NEAR_SCALE = 0.5f;
    // The carrier always rides behind another passenger (ShellCarrier), so it takes the back seat
    private static final double SEAT = -0.6;
    private static final String ROOT = "boat_shell";
    private static final Map<Identifier, Shell> SHELLS = new TreeMap<>();

    private BoatShells() {
    }

    public enum Kind {
        BOAT("boat", false, false),
        CHEST_BOAT("chest_boat", true, false),
        RAFT("raft", false, true),
        CHEST_RAFT("chest_raft", true, true);

        final String folder;
        final boolean chest;
        final boolean raft;

        Kind(String folder, boolean chest, boolean raft) {
            this.folder = folder;
            this.chest = chest;
            this.raft = raft;
        }

        // Seat height over the boat: rideHeight of Boat/ChestBoat (h / 3) and Raft/ChestRaft (h * 0.8888889)
        double rideHeight() {
            return raft ? 0.5625f * 0.8888889f : 0.5625f / 3.0f;
        }

        static @Nullable Kind of(String path) {
            if (path.endsWith("_chest_raft")) return CHEST_RAFT;
            if (path.endsWith("_raft")) return RAFT;
            if (path.endsWith("_chest_boat")) return CHEST_BOAT;
            if (path.endsWith("_boat")) return BOAT;
            return null;
        }
    }

    public record Shell(Kind kind, Identifier wood) {
        Identifier texture() {
            return PolymerPatchBundleMod.id("item/" + ROOT + "/" + wood.getNamespace() + "/" + kind.folder + "/" + wood.getPath());
        }

        Identifier model(boolean near) {
            return PolymerPatchBundleMod.id(ROOT + "/" + wood.getNamespace() + "/" + kind.folder + "/" + wood.getPath() + suffix(near));
        }

        // The stack the carrier holds at that scale; its item never shows, only the item model
        public ItemStack stack(boolean near) {
            ItemStack stack = new ItemStack(Items.STICK);
            stack.set(DataComponents.ITEM_MODEL, model(near));
            return stack;
        }

        private @Nullable IoSupplier<InputStream> source() {
            return ResourceHelper.getAsset(wood.getNamespace(), "textures/entity/" + kind.folder + "/" + wood.getPath() + ".png");
        }
    }

    // Registers the shell of a modded boat entity <ns>:<wood>_[chest_](boat|raft); null without its texture
    public static @Nullable Shell register(Identifier entityId) {
        String path = entityId.getPath();
        Kind kind = Kind.of(path);
        if (kind == null) return null;
        String wood = path.substring(0, path.length() - kind.folder.length() - 1);
        Shell shell = new Shell(kind, Identifier.fromNamespaceAndPath(entityId.getNamespace(), wood));
        if (shell.source() == null) {
            PolymerPatchBundleMod.LOGGER.warn("No texture for boat {}, it shows without a shell", entityId);
            return null;
        }
        synchronized (SHELLS) {
            SHELLS.put(entityId, shell);
        }
        return shell;
    }

    public static void addAssets(ResourcePackBuilder builder) {
        Set<Kind> kinds = EnumSet.noneOf(Kind.class);
        synchronized (SHELLS) {
            for (Shell shell : SHELLS.values()) {
                IoSupplier<InputStream> source = shell.source();
                if (source == null) continue;
                try (InputStream stream = source.get()) {
                    builder.addData(path("textures", shell.texture(), ".png"), stream.readAllBytes());
                } catch (IOException e) {
                    PolymerPatchBundleMod.LOGGER.error("Failed to copy boat texture {}", shell.wood(), e);
                    continue;
                }
                kinds.add(shell.kind());
                for (boolean near : new boolean[]{false, true}) {
                    JsonObject textures = new JsonObject();
                    textures.addProperty("hull", shell.texture().toString());
                    textures.addProperty("particle", shell.texture().toString());
                    JsonObject model = new JsonObject();
                    model.addProperty("parent", variant(shell.kind(), near).toString());
                    model.add("textures", textures);
                    builder.addData(path("models", shell.model(near), ".json"), bytes(model));

                    JsonObject reference = new JsonObject();
                    reference.addProperty("type", "minecraft:model");
                    reference.addProperty("model", shell.model(near).toString());
                    JsonObject definition = new JsonObject();
                    definition.add("model", reference);
                    builder.addData(path("items", shell.model(near), ".json"), bytes(definition));
                }
            }
        }
        for (Kind kind : kinds) {
            JsonObject hull = new JsonObject();
            hull.add("elements", ShellGeometry.elements(kind));
            builder.addData(path("models", hull(kind), ".json"), bytes(hull));
            for (boolean near : new boolean[]{false, true}) {
                JsonObject model = new JsonObject();
                model.addProperty("parent", hull(kind).toString());
                JsonObject display = new JsonObject();
                display.add("ground", groundTransform(kind, near ? NEAR_SCALE : FAR_SCALE));
                model.add("display", display);
                builder.addData(path("models", variant(kind, near), ".json"), bytes(model));
            }
        }
    }

    // Places the hull, baked in item space as (z + 8, y + 8, 8 - x) of BoatModel space, over the boat:
    // undoes the carrier's scale, seat height and seat, and DolphinCarryingItemLayer's (0, 1, -1)
    private static JsonObject groundTransform(Kind kind, float carrierScale) {
        double y = 0.501 - (0.375 - kind.rideHeight()) / carrierScale;
        double z = 1 + SEAT / carrierScale;
        JsonObject ground = new JsonObject();
        ground.add("translation", ShellGeometry.vector(0, y * 16, z * 16));
        ground.add("scale", ShellGeometry.vector(1 / carrierScale, 1 / carrierScale, 1 / carrierScale));
        return ground;
    }

    private static Identifier hull(Kind kind) {
        return PolymerPatchBundleMod.id(ROOT + "/" + kind.folder);
    }

    private static Identifier variant(Kind kind, boolean near) {
        return PolymerPatchBundleMod.id(ROOT + "/" + kind.folder + suffix(near));
    }

    private static String suffix(boolean near) {
        return near ? "_near" : "_far";
    }

    private static String path(String type, Identifier id, String extension) {
        return "assets/" + id.getNamespace() + "/" + type + "/" + id.getPath() + extension;
    }

    private static byte[] bytes(JsonObject json) {
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
}
