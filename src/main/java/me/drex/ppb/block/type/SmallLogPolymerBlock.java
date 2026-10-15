package me.drex.ppb.block.type;

import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.ppb.PolymerPatchBundleMod;
import me.drex.ppb.block.Carriers;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public record SmallLogPolymerBlock(
    Map<Direction.Axis, BlockState> clientBlock) implements FactoryBlock, PolymerTexturedBlock, BSMMParticleBlock, TexturedStates {

    private static final Map<Identifier, ModelAsset> MODELS = new HashMap<>();
    private static final Set<Identifier> USED_TEXTURES = new HashSet<>();

    static {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> {
            MODELS.forEach((id, asset) -> {
                String path = AssetPaths.model(id) + ".json";
                builder.addData(path, asset.toBytes());
            });
            USED_TEXTURES.forEach(id -> {
                String path = AssetPaths.texture(id) + ".png";
                byte[] bytes = builder.getData(path);
                if (bytes == null) {
                    PolymerPatchBundleMod.LOGGER.error("Failed to find texture {}", path);
                    return;
                }
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (id.getPath().contains("_top")) {
                        patchTopTexture(img);
                    } else {
                        patchSideTexture(img);
                    }
                    builder.addData(path, PackResource.fromImage(img));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        });
    }

    private static void patchTopTexture(BufferedImage img) {
        // Collect the colors (in appearing order) of all relevant rings
        List<Integer> innerRingColors = new LinkedList<>();
        List<Integer> outerRingColors = new LinkedList<>();
        List<Integer> barkRingColors = new LinkedList<>();

        int startX = 7;
        int startY = 7;

        int[] dx = new int[]{1, 0, -1, 0};
        int[] dy = new int[]{0, 1, 0, -1};

        for (int ring = 0; ring < 5; ring++) {
            int steps = (ring * 2) + 1;
            int x = startX;
            int y = startY;

            for (int dir = 0; dir < 4; dir++) {
                for (int i = 0; i < steps; i++) {
                    int color = img.getRGB(x, y);
                    if (ring == 4) {
                        barkRingColors.add(color);
                    } else if (ring % 2 == 0) {
                        innerRingColors.add(color);
                    } else {
                        outerRingColors.add(color);
                    }
                    x += dx[dir];
                    y += dy[dir];
                }
            }
            startX--;
            startY--;
        }

        // Expand the ring patterns with the collected color patterns
        startX = 7;
        startY = 7;
        for (int ring = 0; ring < 8; ring++) {
            int steps = (ring * 2) + 1;
            int x = startX;
            int y = startY;

            int index = 0;
            for (int dir = 0; dir < 4; dir++) {
                for (int i = 0; i < steps; i++) {
                    List<Integer> colors;
                    if (ring == 7) {
                        colors = barkRingColors;
                    } else if (ring == 0 || ring == 2 || ring == 5) {
                        colors = innerRingColors;
                    } else {
                        colors = outerRingColors;
                    }
                    img.setRGB(x, y, colors.get(index % colors.size()));

                    x += dx[dir];
                    y += dy[dir];
                    index++;
                }
            }
            startX--;
            startY--;
        }
    }


    private static void patchSideTexture(BufferedImage img) {
        var xo = 0;
        var yo = 0;
        var width = img.getWidth();
        var height = img.getHeight();

        loop:
        for (; xo < width / 2; xo++) {
            for (; yo < height / 2; yo++) {
                if (ARGB.alpha(img.getRGB(xo, yo)) != 0) {
                    break loop;
                }
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (ARGB.alpha(img.getRGB(x, y)) == 0) {
                    img.setRGB(x, y, img.getRGB((x + xo) % width, (y + yo) % height));
                }
            }
        }
    }

    public static SmallLogPolymerBlock of(Identifier id) {
        Map<Direction.Axis, BlockState> clientBlock = new HashMap<>();
        Map<String, ModelAsset.TextureSlot> textures = new HashMap<>();
        if (id.getPath().contains("oak_log")) {
            BlockState state = (id.getPath().contains("stripped") ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_LOG).defaultBlockState();
            for (Direction.Axis value : Direction.Axis.values()) {
                clientBlock.put(value, state.setValue(RotatedPillarBlock.AXIS, value));
            }
            return new SmallLogPolymerBlock(clientBlock);
        } else {
            textures.put("end", new ModelAsset.TextureValue(id.withPrefix("block/").withSuffix("_top"), false));
            textures.put("side", new ModelAsset.TextureValue(id.withPrefix("block/"), false));
        }

        textures.forEach((key, textureSlot) -> USED_TEXTURES.add(((ModelAsset.TextureValue)textureSlot).sprite()));
        {

            ModelAsset modelAsset = new ModelAsset(Optional.of(Identifier.withDefaultNamespace("block/cube_column")), Optional.empty(), textures);
            Identifier modelId = id.withPrefix("block/").withSuffix("_cube_column");
            MODELS.put(modelId, modelAsset);
            Carriers.defineModel(modelId, modelAsset);
            BlockState state = Carriers.request(id, "axis=y", "", new PolymerBlockModel(modelId, 0, 0, false, 1));
            clientBlock.put(Direction.Axis.Y, state);
        }

        {
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                ModelAsset modelAsset = new ModelAsset(Optional.of(Identifier.withDefaultNamespace("block/cube_column_horizontal")), Optional.empty(), textures);
                Identifier modelId = id.withPrefix("block/").withSuffix("_cube_column_horizontal");
                MODELS.put(modelId, modelAsset);
                Carriers.defineModel(modelId, modelAsset);
                BlockState state = Carriers.request(id, "axis=" + axis.getSerializedName(), "", new PolymerBlockModel(modelId, 90, axis == Direction.Axis.X ? 90 : 0, false, 1));
                clientBlock.put(axis, state);
            }
        }

        return new SmallLogPolymerBlock(clientBlock);
    }

    @Override
    public boolean isTextured(BlockState state) {
        return clientBlock.get(state.getValue(SmallLogBlock.AXIS)) != null;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return clientBlock.getOrDefault(state.getValue(SmallLogBlock.AXIS), Blocks.BARRIER.defaultBlockState());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return null;
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
