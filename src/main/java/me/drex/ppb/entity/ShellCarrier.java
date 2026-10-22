package me.drex.ppb.entity;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.SimpleEntityElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.drex.ppb.mixin.MobAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// An invisible dolphin riding a modded boat last, holding the boat's shell; only pack clients see it.
// A living passenger's body turns with the boat every frame, so the held shell stays on the hull.
public final class ShellCarrier extends ElementHolder {
    private static final Scoreboard TEAMS = new Scoreboard();
    // Each watcher's carrier scale is re-picked every this many ticks: near inside 10 blocks, far past 14
    private static final int CHECK_INTERVAL = 10;
    private static final double NEAR_SQR = 10 * 10;
    private static final double FAR_SQR = 14 * 14;
    private final AbstractBoat boat;
    private final SimpleEntityElement dolphin = new SimpleEntityElement(EntityTypes.DOLPHIN);
    // Rides ahead of the dolphin while the boat is empty, so the dolphin never takes the lone seat
    private final ItemDisplayElement placeholder = new ItemDisplayElement();
    private final IntList emptySeats;
    private final IntList riddenSeats;
    // Each carrier scale with the shell variant made for it
    private final List<Packet<? super ClientGamePacketListener>> nearShell;
    private final List<Packet<? super ClientGamePacketListener>> farShell;
    private final ClientboundSetPlayerTeamPacket joinTeam;
    private final ClientboundSetPlayerTeamPacket leaveTeam;
    private final Set<ServerGamePacketListenerImpl> near = new ReferenceOpenHashSet<>();
    private int age;

    public ShellCarrier(AbstractBoat boat, BoatShells.Shell shell) {
        this.boat = boat;
        dolphin.setInvisible(true);
        dolphin.setSilent(true);
        dolphin.getSyncedData().set(MobAccessor.ppb$getMobFlags(), (byte) 1);
        dolphin.ignorePositionUpdates();
        dolphin.setInteractionHandler(VirtualElement.InteractionHandler.redirect(boat));
        placeholder.ignorePositionUpdates();
        placeholder.setInteractionHandler(VirtualElement.InteractionHandler.redirect(boat));
        emptySeats = IntList.of(placeholder.getEntityId(), dolphin.getEntityId());
        riddenSeats = IntList.of(dolphin.getEntityId());
        age = dolphin.getEntityId();

        nearShell = scaleAndShell(BoatShells.NEAR_SCALE, shell.stack(true));
        farShell = scaleAndShell(BoatShells.FAR_SCALE, shell.stack(false));
        // A team that never collides, so the carrier pushes nobody on or beside the boat
        PlayerTeam team = new PlayerTeam(TEAMS, "ppb_shell_" + dolphin.getEntityId());
        team.setCollisionRule(Team.CollisionRule.NEVER);
        team.getPlayers().add(dolphin.getUuid().toString());
        joinTeam = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
        leaveTeam = ClientboundSetPlayerTeamPacket.createRemovePacket(team);

        addPassengerElement(placeholder);
        addPassengerElement(dolphin);
        EntityAttachment.ofTicking(this, boat);
    }

    public static boolean showsTo(ServerPlayer player) {
        return PolymerResourcePackUtils.hasMainPack(player);
    }

    // Read as each passenger packet for the boat is built, so the placeholder leaves and returns with the riders
    @Override
    public IntList getAttachedPassengerEntityIds() {
        return boat.getPassengers().isEmpty() ? emptySeats : riddenSeats;
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (!showsTo(player.player)) return false;
        // Spawn where the boat is now and facing its way, since position updates are off
        dolphin.updateLastSyncedPos();
        placeholder.updateLastSyncedPos();
        dolphin.setYaw(boat.getYRot());
        return super.startWatching(player);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void startWatchingExtraPackets(ServerGamePacketListenerImpl player, Consumer<Packet<ClientGamePacketListener>> packets) {
        boolean close = wantsNear(player.player, false);
        if (close) near.add(player);
        for (Packet<? super ClientGamePacketListener> packet : close ? nearShell : farShell) {
            packets.accept((Packet<ClientGamePacketListener>) packet);
        }
        packets.accept(joinTeam);
    }

    @Override
    public boolean stopWatching(ServerGamePacketListenerImpl player) {
        if (!super.stopWatching(player)) return false;
        near.remove(player);
        if (player.isAcceptingMessages()) player.send(leaveTeam);
        return true;
    }

    @Override
    protected void onTick() {
        if (++age % CHECK_INTERVAL != 0) return;
        for (ServerGamePacketListenerImpl connection : getWatchingPlayers()) {
            boolean was = near.contains(connection);
            boolean now = wantsNear(connection.player, was);
            if (now == was) continue;
            if (now) near.add(connection);
            else near.remove(connection);
            // One bundle, so the scale and the shell change on the same client tick
            connection.send(new ClientboundBundlePacket(now ? nearShell : farShell));
        }
    }

    // Riders and close watchers get the small carrier; the far one draws as far as the boat itself
    private boolean wantsNear(ServerPlayer player, boolean wasNear) {
        if (boat.hasPassenger(player)) return true;
        double distance = player.distanceToSqr(boat);
        return distance < NEAR_SQR || wasNear && distance <= FAR_SQR;
    }

    private List<Packet<? super ClientGamePacketListener>> scaleAndShell(float scale, ItemStack stack) {
        AttributeInstance attribute = new AttributeInstance(Attributes.SCALE, instance -> {
        });
        attribute.setBaseValue(scale);
        return List.of(
            new ClientboundUpdateAttributesPacket(dolphin.getEntityId(), List.of(attribute)),
            // A dragon head (which no dolphin layer draws) widens the client's culling box by 0.5, so the shell
            // isn't culled while the driver, sitting ahead of the small carrier, can see the hull
            new ClientboundSetEquipmentPacket(dolphin.getEntityId(), List.of(Pair.of(EquipmentSlot.MAINHAND, stack),
                Pair.of(EquipmentSlot.HEAD, new ItemStack(Items.DRAGON_HEAD))))
        );
    }
}
