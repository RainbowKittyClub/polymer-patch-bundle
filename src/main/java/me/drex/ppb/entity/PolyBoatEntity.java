package me.drex.ppb.entity;

import com.google.common.base.Suppliers;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.drex.ppb.mixin.AbstractBoatAccessor;
import me.drex.ppb.mixin.VehicleEntityAccessor;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// One modded boat as clients see it: the nearest-wood stand-in boat, with a shell carrier for pack clients
public final class PolyBoatEntity implements PolymerEntity {
    // Hurt time, hurt direction, damage and bubble time: the hit wobble and bubble rocking tip the hull through the shell
    private static final IntSet ROCKING = IntSet.of(VehicleEntityAccessor.ppb$getHurt().id(), VehicleEntityAccessor.ppb$getHurtDir().id(),
        VehicleEntityAccessor.ppb$getDamage().id(), AbstractBoatAccessor.ppb$getBubbleTime().id());
    private static final List<Supplier<EntityType<?>>> STAND_INS = new ArrayList<>();
    private final Supplier<EntityType<?>> standIn;
    private final @Nullable ShellCarrier carrier;

    private PolyBoatEntity(Supplier<EntityType<?>> standIn, AbstractBoat boat, BoatShells.@Nullable Shell shell) {
        this.standIn = standIn;
        this.carrier = shell != null && boat.level() instanceof ServerLevel ? new ShellCarrier(boat, shell) : null;
    }

    public static void register(EntityType<?> type, Identifier entityId) {
        Supplier<EntityType<?>> standIn = Suppliers.memoize(() -> PolyBaseEntity.createBoat(entityId).entityType());
        BoatShells.Shell shell = BoatShells.register(entityId);
        STAND_INS.add(standIn);
        PolymerEntityUtils.registerOverlay(type, boat -> new PolyBoatEntity(standIn, (AbstractBoat) boat, shell));
    }

    // Polymer caches tracked-data templates unsynchronised, from Netty threads too; fill ours on the
    // server thread first, so a crowd of new boats can't race it into a kick
    public static void warmTrackedData() {
        PolymerEntityUtils.getDefaultSynchedEntityData(EntityTypes.DOLPHIN);
        PolymerEntityUtils.getDefaultSynchedEntityData(EntityTypes.ITEM_DISPLAY);
        for (Supplier<EntityType<?>> standIn : STAND_INS) {
            PolymerEntityUtils.getDefaultSynchedEntityData(standIn.get());
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return standIn.get();
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        if (carrier != null && player != null && ShellCarrier.showsTo(player)) {
            data.removeIf(value -> ROCKING.contains(value.id()));
        }
    }
}
