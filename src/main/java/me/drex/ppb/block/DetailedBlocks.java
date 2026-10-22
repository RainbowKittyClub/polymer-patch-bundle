package me.drex.ppb.block;

import com.mojang.authlib.GameProfile;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.drex.ppb.shaded.kalic0re.config.ModConfig;
import me.drex.ppb.shaded.kalic0re.config.Setting;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player settings that draw thin blocks (small logs, saguaro) at their real size with a display entity
 * each, instead of a full-block stand-in; one per kind, since a sakura forest holds far more of them than
 * the rest. Settings of the rkcore-common settings hub, or of ppb's own {@code /settings} without the hub.
 */
public final class DetailedBlocks {
    private static final ModConfig CONFIG = ModConfig.create("polymer-patch-bundle");
    // Read on netty threads while chunks are written, so kept apart from the settings store
    private static final Map<UUID, Set<Kind>> SHOWN = new ConcurrentHashMap<>();

    public enum Kind {
        SMALL_OAK("detailed_small_oak_logs", true),
        SAKURA("detailed_sakura_logs", false),
        YUCCA_PALM("detailed_yucca_palm_logs", true),
        SAGUARO("detailed_saguaro_cacti", true);

        private final Setting<Boolean> setting;

        Kind(String key, boolean defaultValue) {
            this.setting = CONFIG.toggle(key, defaultValue).player();
        }

        // Terrestria's small log woods; other Terraform woods share the small oak setting
        public static Kind of(Identifier blockId) {
            String path = blockId.getPath();
            if (path.contains("sakura")) return SAKURA;
            if (path.contains("yucca_palm")) return YUCCA_PALM;
            if (path.contains("saguaro")) return SAGUARO;
            return SMALL_OAK;
        }
    }

    private DetailedBlocks() {
    }

    public static void register() {
        // Declares every kind's setting; the config must hold them all before it registers
        Kind[] kinds = Kind.values();
        CONFIG.register();
        // Before the first chunk goes out; the hub has loaded the player's values during configuration
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            Set<Kind> shown = EnumSet.noneOf(Kind.class);
            for (Kind kind : kinds) {
                if (kind.setting.get(handler.player)) {
                    shown.add(kind);
                }
            }
            SHOWN.put(handler.player.getUUID(), shown);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SHOWN.remove(handler.player.getUUID()));
        for (Kind kind : kinds) {
            kind.setting.onChange((server, player, value) -> {
                if (player != null && update(player, kind, value)) {
                    if (!value) {
                        SmallLogModel.hideFrom(player, kind);
                    }
                    PolymerUtils.reloadLevel(player);
                }
            });
        }
    }

    /**
     * Whether this packet's player sees this kind drawn by displays: its setting is on and the player has the
     * resource pack those displays need.
     */
    public static boolean shows(@Nullable PacketContext context, Kind kind) {
        GameProfile profile = context != null ? context.get(PacketContext.GAME_PROFILE) : null;
        Set<Kind> shown = profile != null ? SHOWN.get(profile.id()) : null;
        return shown != null && shown.contains(kind) && PolymerResourcePackUtils.hasMainPack(context);
    }

    public static boolean shows(ServerPlayer player, Kind kind) {
        return shows(player.connection != null ? player.connection.getPacketContext() : null, kind);
    }

    // Whether the player's choice changed; sets are replaced, never edited, so readers never see one half-written
    private static boolean update(ServerPlayer player, Kind kind, boolean shown) {
        Set<Kind> old = SHOWN.getOrDefault(player.getUUID(), Set.of());
        if (old.contains(kind) == shown) {
            return false;
        }
        Set<Kind> next = old.isEmpty() ? EnumSet.noneOf(Kind.class) : EnumSet.copyOf(old);
        if (shown) next.add(kind); else next.remove(kind);
        SHOWN.put(player.getUUID(), next);
        return true;
    }
}
