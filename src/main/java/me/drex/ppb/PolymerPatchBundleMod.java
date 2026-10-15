package me.drex.ppb;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.impl.DefaultModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.MapColorTintSource;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.impl.HolderHolder;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.drex.ppb.color.ColorProviderRegistry;
import me.drex.ppb.color.ColorMapHelper;
import me.drex.ppb.mixson.CinderscapesArmorTrimItemModels;
import me.drex.ppb.mixson.MixsonPatcher;
import me.drex.ppb.res.ResourceHelper;
import me.drex.ppb.res.ResourcePackGenerator;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static net.minecraft.commands.Commands.literal;

public class PolymerPatchBundleMod implements ModInitializer {
    public static final String MOD_ID = "polymer-patch-bundle";

    public static final Set<String> MOD_NAMESPACES = Set.of("blockus", "terrestria", "cinderscapes", "traverse", "woods_and_mires");
    public static final Set<String> MOD_ASSET_IDS = Set.of("blockus", "terrestria", "cinderscapes", "traverse", "woods_and_mires", "terraform");

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(ResourceHelper::init);
        ResourcePackGenerator.setup();
        MixsonPatcher.setup();
        if (FabricLoader.getInstance().isModLoaded("cinderscapes")) {
            CinderscapesArmorTrimItemModels.init();
        }
        ColorMapHelper.init();
        ColorProviderRegistry.init();
        // Textured full blocks are note block states, so clients play wood sounds for all of them;
        // the server sends each block's real sounds instead
        // Covers every wood-sound block, vanilla's too: a player hears their own a round trip late, and client sound mods lose them
        SoundPatcher.convertIntoServerSound(SoundType.WOOD);

        for (String modNamespace : MOD_NAMESPACES) {
            setupModAssets(modNamespace);
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, a, b) -> {
                dispatcher.register(literal("count_displays").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    var map = new Reference2IntOpenHashMap<Block>();
                    for (var holder : ((HolderHolder) player.connection).polymer$getHolders()) {
                        if (holder.getAttachment() instanceof BlockAwareAttachment attachment) {
                            map.put(attachment.getBlockState().getBlock(), map.getInt(attachment.getBlockState().getBlock()) + 1);
                        }
                    }
                    var entries = new ArrayList<>(map.reference2IntEntrySet());
                    entries.sort(Comparator.comparing(Reference2IntMap.Entry::getIntValue));
                    for (var entry : entries.reversed()) {
                        player.sendSystemMessage(Component.literal(BuiltInRegistries.BLOCK.getKey(entry.getKey()) + " -> " + entry.getIntValue()));
                    }

                    return 0;
                }));
                dispatcher.register(literal("count_model_types").executes(ctx -> {
                    for (var entry : BlockModelType.values()) {
                        ctx.getSource().sendSystemMessage(Component.literal(entry.name() + " -> " + PolymerBlockResourceUtils.getBlocksLeft(entry) + " / " + DefaultModelData.USABLE_STATES.get(entry).size()));
                    }

                    return 0;
                }));
            });
        }
    }


    public static void setupModAssets(String modid) {
        ResourcePackExtras.forDefault().addBridgedModelsFolder(
            Identifier.fromNamespaceAndPath(modid, "block"),
            (id, resourcePackBuilder) -> {
                return new ItemAsset(new BasicItemModel(id, List.of(new MapColorTintSource(0xFFFFFF))), ItemAsset.Properties.DEFAULT);
            }
        );
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
