package me.drex.ppb.block;

import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.drex.ppb.color.ColorProviderRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockStateModel extends BlockModel {
    private final List<ItemDisplayElement> modelElements = new ArrayList<>();
    private final float viewRange;
    private boolean shifty = false;

    public BlockStateModel(float viewRange) {
        this.viewRange = viewRange;
    }

    public static BlockStateModel longRange() {
        return new BlockStateModel(100.0F);
    }

    public static BlockStateModel midRange() {
        return new BlockStateModel(3.0F);
    }

    public static BlockStateModel shortRange() {
        return new BlockStateModel(1.1F);
    }

    public BlockStateModel shifty() {
        shifty = true;
        return this;
    }

    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        super.notifyUpdate(updateType);
        if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
            this.applyModel(BlockStateModelManager.get(this.blockState()), this.blockPos());
        }
    }

    @Override
    protected void onAttachmentSet(HolderAttachment attachment, @Nullable HolderAttachment oldAttachment) {
        super.onAttachmentSet(attachment, oldAttachment);
        this.applyModel(BlockStateModelManager.get(this.blockState()), this.blockPos());
    }

    private void applyModel(List<BlockStateModelManager.ModelGetter> models, BlockPos pos) {
        RandomSource random = RandomSource.create(this.blockState().getSeed(pos));
        int i = 0;

        while (models.size() < this.modelElements.size()) {
            this.removeElement(this.modelElements.removeLast());
        }

        for (; i < models.size(); ++i) {
            boolean newModel = false;
            ItemDisplayElement element;
            if (this.modelElements.size() <= i) {
                element = ItemDisplayElementUtil.createSimple();
                element.setViewRange(this.viewRange);
                element.setTeleportDuration(0);
                element.setItemDisplayContext(ItemDisplayContext.NONE);
                element.setYaw(180.0F);
                newModel = true;
                this.modelElements.add(element);
            } else {
                element = this.modelElements.get(i);
            }

            BlockStateModelManager.ModelData model = models.get(i).getModel(random);
            element.setItem(model.stack());
            element.setLeftRotation(model.quaternionfc());
            this.setupElement(element, i, pos);
            if (newModel) {
                this.addElement(element);
            } else {
                element.tick();
            }
        }
    }

    protected void setupElement(ItemDisplayElement element, int i, BlockPos pos) {
        if (!(getAttachment() instanceof BlockBoundAttachment blockBoundAttachment)) {
            return;
        }
        int color = ColorProviderRegistry.getColor(this.blockState(), blockBoundAttachment.getChunk(), pos);
        if (color != -1) {
            ItemStack item = element.getItem().copy();
            item.set(DataComponents.MAP_COLOR, new MapItemColor(color));
            // 26.3 clients tint from custom_model_data colours only; setting them on this copy spares
            // a 26.3 compat layer copying the stack again
            CustomModelData data = item.get(DataComponents.CUSTOM_MODEL_DATA);
            if (data == null || data.colors().isEmpty()) {
                item.set(DataComponents.CUSTOM_MODEL_DATA, data == null
                    ? new CustomModelData(List.of(), List.of(), List.of(), List.of(color))
                    : new CustomModelData(data.floats(), data.flags(), data.strings(), List.of(color)));
            }
            element.setItem(item);
        }

        if (shifty) {
            i = Math.abs((i + pos.getX() + pos.getY() + pos.getZ()) % 5);
            element.setOffset(new Vec3(i / 5000f, i / 5000f, i / 5000f).subtract(5 / 5000f / 2));
        }
    }
}
