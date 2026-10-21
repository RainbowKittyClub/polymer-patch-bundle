package me.drex.ppb.mixin.terraform;

import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.terraform.boat.impl.item.TerraformBoatItemHelperImpl;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import me.drex.ppb.entity.PolyBaseEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(TerraformBoatItemHelperImpl.class)
public abstract class TerraformBoatItemHelperImplMixin {
    @Inject(method = "registerBoat", at = @At("RETURN"))
    private static <T extends AbstractBoat> void polymerifyBoats(
        Identifier id, ResourceKey<Item> itemKey, ResourceKey<EntityType<?>> entityTypeKey,
        Item.Properties settings, Function<Supplier<Item>, EntityType.EntityFactory<T>> factory,
        BiConsumer<Identifier, EntityType<T>> registry, CallbackInfoReturnable<BoatItem> cir,
        @Local(name = "entityType") EntityType<?> entityType, @Local(name = "item") BoatItem item
    ) {
        Identifier location = entityTypeKey.identifier();
        PolymerEntityUtils.registerOverlay(entityType, object -> PolyBaseEntity.createBoat(location));
    }
}
