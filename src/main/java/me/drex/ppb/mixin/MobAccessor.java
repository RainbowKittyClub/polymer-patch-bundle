package me.drex.ppb.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobAccessor {
    // Bit 0 is NoAI
    @Accessor("DATA_MOB_FLAGS_ID")
    static EntityDataAccessor<Byte> ppb$getMobFlags() {
        throw new AssertionError();
    }
}
