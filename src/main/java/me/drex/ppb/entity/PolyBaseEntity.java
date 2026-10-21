package me.drex.ppb.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import me.drex.ppb.res.VanillaWoods;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public record PolyBaseEntity(EntityType<?> entityType) implements PolymerEntity {

    // A modded boat shows as the boat of the vanilla wood that looks most like it; rafts as bamboo
    public static PolyBaseEntity createBoat(Identifier entityId) {
        String path = entityId.getPath();
        boolean chest = path.contains("_chest");
        if (path.contains("_raft")) {
            return new PolyBaseEntity(chest ? EntityTypes.BAMBOO_CHEST_RAFT : EntityTypes.BAMBOO_RAFT);
        }
        String wood = path.replaceFirst("(_chest)?_boat$", "");
        String vanilla = VanillaWoods.nearest(Identifier.fromNamespaceAndPath(entityId.getNamespace(), wood));
        EntityType<?> fallback = chest ? EntityTypes.BIRCH_CHEST_BOAT : EntityTypes.BIRCH_BOAT;
        EntityType<?> boat = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(vanilla + (chest ? "_chest_boat" : "_boat"))).orElse(fallback);
        return new PolyBaseEntity(boat);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return entityType;
    }
}
