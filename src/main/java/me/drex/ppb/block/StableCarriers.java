package me.drex.ppb.block;

import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import me.drex.ppb.PolymerPatchBundleMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Blocks that get one of Polymer's stable full block slots, whose client-side state survives the
 * client's own block updates, so breaking or placing next to a tree trunk or a stretch of terrain
 * doesn't show other blocks along its column until the server answers. Natural upright trunks come
 * first, then the most visible terrain. Slots are fixed so the pack never depends on registration
 * order; enderscape-polymer-patch holds 21, 24, 25, 28, 32-39, 41-43, 46-48 and sensible-storage
 * 17. Snowless andisol grass is left out: it is drawn on a leaves state, which these updates never
 * change.
 */
public final class StableCarriers {
    private static final List<Entry> ENTRIES = List.of(
        entry(0, "terrestria:redwood_quarter_log", "axis=y,bark_side=northeast"),
        entry(1, "terrestria:redwood_quarter_log", "axis=y,bark_side=northwest"),
        entry(2, "terrestria:redwood_quarter_log", "axis=y,bark_side=southeast"),
        entry(3, "terrestria:redwood_quarter_log", "axis=y,bark_side=southwest"),
        entry(4, "terrestria:hemlock_quarter_log", "axis=y,bark_side=northeast"),
        entry(5, "terrestria:hemlock_quarter_log", "axis=y,bark_side=northwest"),
        entry(6, "terrestria:hemlock_quarter_log", "axis=y,bark_side=southeast"),
        entry(7, "terrestria:hemlock_quarter_log", "axis=y,bark_side=southwest"),
        entry(8, "terrestria:redwood_log", "axis=y"),
        entry(9, "terrestria:hemlock_log", "axis=y"),
        entry(10, "traverse:fir_log", "axis=y"),
        entry(11, "woods_and_mires:pine_log", "axis=y"),
        entry(12, "woods_and_mires:aged_pine_log", "axis=y,mid=false"),
        entry(13, "terrestria:rainbow_eucalyptus_quarter_log", "axis=y,bark_side=southeast"),
        entry(14, "terrestria:cypress_log", "axis=y"),
        entry(15, "terrestria:cypress_quarter_log", "axis=y,bark_side=northeast"),
        entry(16, "terrestria:cypress_quarter_log", "axis=y,bark_side=northwest"),
        entry(18, "terrestria:cypress_quarter_log", "axis=y,bark_side=southwest"),
        entry(19, "terrestria:rainbow_eucalyptus_quarter_log", "axis=y,bark_side=northeast"),
        entry(20, "terrestria:rainbow_eucalyptus_quarter_log", "axis=y,bark_side=northwest"),
        entry(22, "terrestria:rainbow_eucalyptus_quarter_log", "axis=y,bark_side=southwest"),
        entry(23, "terrestria:rainbow_eucalyptus_log", "axis=y"),
        entry(26, "cinderscapes:umbral_stem", "axis=y"),
        entry(27, "cinderscapes:scorched_stem", "axis=y"),
        entry(29, "terrestria:andisol", ""),
        entry(30, "terrestria:volcanic_rock", ""),
        entry(31, "terrestria:volcanic_sand", ""),
        entry(40, "cinderscapes:umbral_nylium", ""),
        entry(44, "terrestria:andisol_grass_block", "snowy=true"),
        entry(45, "cinderscapes:umbral_wart_block", "")
    );
    private static final @Nullable MethodHandle REQUEST_STABLE_BLOCK = findRequestStableBlock();

    private StableCarriers() {
    }

    /**
     * The stable state for this block state, or null when it has no slot, the slot is taken, or
     * Polymer has no stable slots; the caller then requests a normal state.
     */
    public static @Nullable BlockState request(Identifier id, BlockState state, PolymerBlockModel[] model) {
        if (REQUEST_STABLE_BLOCK == null) {
            return null;
        }
        for (Entry entry : ENTRIES) {
            if (entry.block.equals(id) && entry.matches(state)) {
                try {
                    var stable = (BlockState) REQUEST_STABLE_BLOCK.invokeExact(entry.slot, model);
                    if (stable == null) {
                        PolymerPatchBundleMod.LOGGER.warn("Stable slot {} for {} is taken", entry.slot, id);
                    }
                    return stable;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }

    private static Entry entry(int slot, String block, String properties) {
        var values = new ArrayList<Map.Entry<String, String>>();
        for (String pair : properties.split(",")) {
            if (!pair.isEmpty()) {
                var split = pair.split("=", 2);
                values.add(Map.entry(split[0], split[1]));
            }
        }
        return new Entry(slot, Identifier.parse(block), List.copyOf(values));
    }

    // Polymer without stable slots: every block falls back to a normal request
    private static @Nullable MethodHandle findRequestStableBlock() {
        try {
            return MethodHandles.publicLookup().findStatic(PolymerBlockResourceUtils.class, "requestStableBlock",
                MethodType.methodType(BlockState.class, int.class, PolymerBlockModel[].class));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private record Entry(int slot, Identifier block, List<Map.Entry<String, String>> properties) {
        boolean matches(BlockState state) {
            for (var value : this.properties) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(value.getKey());
                if (property == null || !value.getValue().equals(name(property, state))) {
                    return false;
                }
            }
            return true;
        }

        private static <T extends Comparable<T>> String name(Property<T> property, BlockState state) {
            return property.getName(state.getValue(property));
        }
    }
}
