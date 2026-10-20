package me.drex.ppb.mixson;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MixsonPatcher {
    private static final PackLocationInfo INFO = new PackLocationInfo("vanilla", Component.translatable("resourcePack.vanilla.name"), PackSource.BUILT_IN, Optional.of(KnownPack.vanilla("resourcepacks")));

    public static void setup() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            var pack = new VanillaPackResourcesBuilder().build(INFO).fullResources();

            Map<Identifier, Resource> attachedResources = new HashMap<>();
            PathPackResources.listPath(Identifier.DEFAULT_NAMESPACE, PolymerCommonUtils.getClientJarRoot().resolve("assets").resolve(Identifier.DEFAULT_NAMESPACE), List.of(), (identifier, ioSupplier) -> {
                attachedResources.put(identifier, new Resource(pack, ioSupplier));
            });

            var hook = new CapturingHook(attachedResources);
            Mixson.processHook(hook);
            hook.overrides.forEach((id, res) -> {
                try {
                    builder.addData("assets/" + id.getNamespace() + "/" + id.getPath(), res.open().readAllBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
