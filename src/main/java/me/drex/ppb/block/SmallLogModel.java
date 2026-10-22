package me.drex.ppb.block;

import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.drex.ppb.block.type.SmallLogPolymerBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The one display that draws a small log over its chorus plant stand-in, shown only to players who get that
 * stand-in (see {@link DetailedBlocks}).
 */
public class SmallLogModel extends BlockModel {
    // Weak, so unloaded blocks' holders go with their chunks
    private static final Set<SmallLogModel> LIVE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private Identifier blockId;
    private final DetailedBlocks.Kind kind;
    private final ItemDisplayElement element = new ItemDisplayElement();

    public SmallLogModel(Identifier blockId, DetailedBlocks.Kind kind) {
        this.blockId = blockId;
        this.kind = kind;
        this.element.setViewRange(3.0F);
        this.element.setTeleportDuration(0);
        this.element.setItemDisplayContext(ItemDisplayContext.NONE);
        this.element.setYaw(180.0F);
        this.addElement(this.element);
        LIVE.add(this);
    }

    /**
     * Stops showing every small log display of this kind to the player. A level reload keeps the client's entities, so
     * turning the setting off has to remove them itself. Server thread only.
     */
    public static void hideFrom(ServerPlayer player, DetailedBlocks.Kind kind) {
        List<SmallLogModel> holders;
        synchronized (LIVE) {
            holders = List.copyOf(LIVE);
        }
        for (SmallLogModel holder : holders) {
            if (holder.kind == kind && holder.getWatchingPlayers().contains(player.connection)) {
                holder.stopWatching(player.connection);
            }
        }
    }

    public DetailedBlocks.Kind kind() {
        return this.kind;
    }

    public Identifier blockId() {
        return this.blockId;
    }

    public ItemDisplayElement element() {
        return this.element;
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        return DetailedBlocks.shows(player.player, this.kind) && super.startWatching(player);
    }

    @Override
    protected void onAttachmentSet(HolderAttachment attachment, @Nullable HolderAttachment oldAttachment) {
        super.onAttachmentSet(attachment, oldAttachment);
        this.update();
        // Same tiny per-position offset as other display-drawn blocks, against z-fighting between neighbours
        var pos = this.blockPos();
        int i = Math.abs((pos.getX() + pos.getY() + pos.getZ()) % 5);
        this.element.setOffset(new Vec3(i / 5000f, i / 5000f, i / 5000f).subtract(5 / 5000f / 2));
    }

    @Override
    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        super.notifyUpdate(updateType);
        if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
            this.update();
        }
    }

    private void update() {
        BlockState state = this.blockState();
        // A holder kept across a strip (SmallLogPolymerBlock.keepElementHolder) draws the new block's log
        if (BlockWithElementHolder.get(state) instanceof SmallLogPolymerBlock block && block.displayed() != null) {
            this.blockId = block.displayed();
        }
        this.element.setItem(SmallLogDisplays.stack(this.blockId, state));
        // A leafy log's leaves stand-in is a full cube; scaled up a little, the branch shows through its faces
        this.element.setScale(new Vector3f(state.getBlock() instanceof SmallLogBlock && state.getValue(SmallLogBlock.HAS_LEAVES) ? 1.01F : 1.0F));
        this.element.tick();
    }
}
