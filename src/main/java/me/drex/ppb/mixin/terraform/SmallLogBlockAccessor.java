package me.drex.ppb.mixin.terraform;

import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmallLogBlock.class)
public interface SmallLogBlockAccessor {
    @Accessor("leaves")
    Block ppb$getLeaves();
}
