package me.drex.ppb.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBoat.class)
public interface AbstractBoatAccessor {
    @Accessor("DATA_ID_BUBBLE_TIME")
    static EntityDataAccessor<Integer> ppb$getBubbleTime() {
        throw new AssertionError();
    }
}
