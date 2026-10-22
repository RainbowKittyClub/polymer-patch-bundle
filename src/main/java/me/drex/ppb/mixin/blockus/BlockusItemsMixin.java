package me.drex.ppb.mixin.blockus;

import com.brand.blockus.registry.content.BlockusItems;
import me.drex.ppb.entity.PolyBoatEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockusItems.class)
public class BlockusItemsMixin {
    @Inject(method = "registerBoat", at = @At("RETURN"))
    private static void polymerifyBoats(String name, EntityType<? extends @NotNull AbstractBoat> entityType, CallbackInfoReturnable<Item> cir) {
        PolyBoatEntity.register(entityType, Identifier.fromNamespaceAndPath("blockus", name));
    }
}
