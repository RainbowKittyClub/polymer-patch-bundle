package me.drex.ppb.block;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import me.drex.ppb.res.ResourceHelper;
import org.jspecify.annotations.Nullable;

// The one file of this package that differs between its three copies: the copy's instruments and
// where it reads models, blockstates and textures.
final class CarrierHost implements CarrierColours.Assets {

    // This copy's instruments in an ordinary group.
    static final Set<String> INSTRUMENTS = CarrierTiers.PATCH_BUNDLE_INSTRUMENTS;

    // An asset from the mods this fork patches and every other mod, else the vanilla client jar.
    @Override
    public @Nullable InputStream open(String namespace, String path) throws IOException {
        var asset = ResourceHelper.getAsset(namespace, path);
        return asset == null ? null : asset.get();
    }
}
