package me.drex.ppb.res;

import me.drex.ppb.PolymerPatchBundleMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The vanilla wood whose planks look most like a modded wood's, for boat and sign stand-ins
public final class VanillaWoods {
    // Woods with boats and all four sign kinds on every client version stand-ins are sent to
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "mangrove", "pale_oak");
    private static final String FALLBACK = "birch";
    private static final Map<Identifier, String> NEAREST = new ConcurrentHashMap<>();
    private static Map<String, double[]> vanillaColours;

    private VanillaWoods() {
    }

    public static String nearest(Identifier wood) {
        return NEAREST.computeIfAbsent(wood, VanillaWoods::find);
    }

    private static String find(Identifier wood) {
        double[] colour = planksColour(wood.getNamespace(), wood.getPath());
        if (colour == null) {
            PolymerPatchBundleMod.LOGGER.warn("No planks texture for wood {}, using {} stand-ins", wood, FALLBACK);
            return FALLBACK;
        }
        String best = FALLBACK;
        double bestDistance = Double.MAX_VALUE;
        for (var entry : vanillaColours().entrySet()) {
            double distance = 0;
            for (int i = 0; i < 3; i++) {
                distance += Math.pow(colour[i] - entry.getValue()[i], 2);
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }
        PolymerPatchBundleMod.LOGGER.info("Wood {} uses {} boat and sign stand-ins", wood, best);
        return best;
    }

    private static synchronized Map<String, double[]> vanillaColours() {
        if (vanillaColours == null) {
            var colours = new LinkedHashMap<String, double[]>();
            for (String wood : WOODS) {
                double[] colour = planksColour(Identifier.DEFAULT_NAMESPACE, wood);
                if (colour != null) colours.put(wood, colour);
            }
            vanillaColours = colours;
        }
        return vanillaColours;
    }

    // Mean CIELAB colour of the opaque pixels of <wood>_planks, or null without the texture
    private static double[] planksColour(String namespace, String wood) {
        IoSupplier<InputStream> supplier = ResourceHelper.getAsset(namespace, "textures/block/" + wood + "_planks.png");
        if (supplier == null) return null;
        try (InputStream stream = supplier.get()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) return null;
            double r = 0, g = 0, b = 0;
            int count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) < 128) continue;
                    r += linear((argb >> 16) & 0xFF);
                    g += linear((argb >> 8) & 0xFF);
                    b += linear(argb & 0xFF);
                    count++;
                }
            }
            if (count == 0) return null;
            return lab(r / count, g / count, b / count);
        } catch (IOException e) {
            PolymerPatchBundleMod.LOGGER.error("Failed to read planks texture {}:{}", namespace, wood, e);
            return null;
        }
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    // Linear sRGB to CIELAB (D65), so distances follow how different colours look
    private static double[] lab(double r, double g, double b) {
        double x = f((0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047);
        double y = f(0.2126 * r + 0.7152 * g + 0.0722 * b);
        double z = f((0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883);
        return new double[]{116 * y - 16, 500 * (x - y), 200 * (y - z)};
    }

    private static double f(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }
}
