package me.drex.ppb.block.type;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A Polymer block that sends some of its states as states the client draws itself, with no display
 * entity, so those states keep the block's own occlusion.
 */
public interface TexturedStates {
    boolean isTextured(BlockState state);
}
