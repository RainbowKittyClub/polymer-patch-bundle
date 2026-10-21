package me.drex.ppb.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VehicleEntity.class)
public interface VehicleEntityAccessor {
    @Accessor("DATA_ID_HURT")
    static EntityDataAccessor<Integer> ppb$getHurt() {
        throw new AssertionError();
    }

    @Accessor("DATA_ID_HURTDIR")
    static EntityDataAccessor<Integer> ppb$getHurtDir() {
        throw new AssertionError();
    }

    @Accessor("DATA_ID_DAMAGE")
    static EntityDataAccessor<Float> ppb$getDamage() {
        throw new AssertionError();
    }
}
