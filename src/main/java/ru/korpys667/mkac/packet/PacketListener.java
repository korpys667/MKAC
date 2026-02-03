/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
 *
 * MKAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MKAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.mkac.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.entity.Player;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.utils.data.Pair;
import ru.korpys667.mkac.utils.update.RotationUpdate;

public class PacketListener extends PacketListenerAbstract {
  private final PlayerDataManager playerDataManager;

  public PacketListener(PlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  private boolean checkTeleportQueue(MKPlayer player, WrapperPlayClientPlayerFlying flying) {
    if (!flying.hasPositionChanged() || player.getPendingTeleports().isEmpty()) {
      return false;
    }

    MKPlayer.TeleportData teleport;
    while ((teleport = player.getPendingTeleports().peek()) != null) {
      if (player.getLastTransactionReceived().get() < teleport.getTransactionId()) {
        break;
      }

      Location flyingLocation = flying.getLocation();
      RelativeFlag flags = teleport.getFlags();

      double expectedX =
          flags.has(RelativeFlag.X)
              ? player.x + teleport.getLocation().getX()
              : teleport.getLocation().getX();
      double expectedY =
          flags.has(RelativeFlag.Y)
              ? player.y + teleport.getLocation().getY()
              : teleport.getLocation().getY();
      double expectedZ =
          flags.has(RelativeFlag.Z)
              ? player.z + teleport.getLocation().getZ()
              : teleport.getLocation().getZ();

      final double epsilon = 1.0E-7;
      if (Math.abs(flyingLocation.getX() - expectedX) < epsilon
          && Math.abs(flyingLocation.getY() - expectedY) < epsilon
          && Math.abs(flyingLocation.getZ() - expectedZ) < epsilon) {

        player.getPendingTeleports().poll();
        return true;
      }

      if (player.getLastTransactionReceived().get() > teleport.getTransactionId()) {
        player.getPendingTeleports().poll();
        continue;
      }
      break;
    }
    return false;
  }

  private boolean checkRotationQueue(MKPlayer player, WrapperPlayClientPlayerFlying flying) {
    if (!flying.hasRotationChanged()
        || flying.hasPositionChanged()
        || player.getPendingRotations().isEmpty()) {
      return false;
    }

    MKPlayer.RotationData rotation;
    while ((rotation = player.getPendingRotations().peek()) != null) {
      if (player.getLastTransactionReceived().get() < rotation.getTransactionId()) {
        break;
      }

      if (flying.getLocation().getYaw() == rotation.getYaw()
          && flying.getLocation().getPitch() == rotation.getPitch()) {
        player.getPendingRotations().poll();
        return true;
      }

      if (player.getLastTransactionReceived().get() > rotation.getTransactionId()) {
        player.getPendingRotations().poll();
        continue;
      }
      break;
    }
    return false;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }

    MKPlayer mkPlayer = playerDataManager.getPlayer((Player) event.getPlayer());
    if (mkPlayer == null) return;

    if (handleTransaction(event, mkPlayer)) {
      return;
    }

    if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
      handleFlying(event, mkPlayer);
    }

    if (event.isCancelled()) {
      resetFlags(mkPlayer);
      return;
    }

    if (mkPlayer.packetStateData.lastPacketWasTeleport
        || mkPlayer.packetStateData.lastPacketWasServerRotation) {
      updatePlayerState(mkPlayer, new WrapperPlayClientPlayerFlying(event));
    }

    mkPlayer.getCheckManager().onPacketReceive(event);

    resetFlags(mkPlayer);
  }

  private boolean handleTransaction(PacketReceiveEvent event, MKPlayer mkPlayer) {
    short id;
    if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
      WrapperPlayClientWindowConfirmation transaction =
          new WrapperPlayClientWindowConfirmation(event);
      id = transaction.getActionId();
      if (id <= 0 && addTransactionResponse(mkPlayer, id)) {
        event.setCancelled(true);
      }
      return true;
    } else if (event.getPacketType() == PacketType.Play.Client.PONG) {
      WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
      id = (short) pong.getId();
      if (addTransactionResponse(mkPlayer, id)) {
        event.setCancelled(true);
      }
      return true;
    }
    return false;
  }

  private void handleFlying(PacketReceiveEvent event, MKPlayer mkPlayer) {
    WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

    boolean teleported = checkTeleportQueue(mkPlayer, flying);
    boolean serverRotated = !teleported && checkRotationQueue(mkPlayer, flying);

    mkPlayer.packetStateData.lastPacketWasTeleport = teleported;
    mkPlayer.packetStateData.lastPacketWasServerRotation = serverRotated;

    isMojangStupid(mkPlayer, flying, event);

    if (!event.isCancelled()) {
      processRotation(mkPlayer, flying);
    }
  }

  private void processRotation(MKPlayer mkPlayer, WrapperPlayClientPlayerFlying packet) {
    boolean ignoreRotation =
        mkPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate
            && mkPlayer.packetStateData.ignoreDuplicatePacketRotation;

    if (packet.hasPositionChanged()) {
      mkPlayer.x = packet.getLocation().getX();
      mkPlayer.y = packet.getLocation().getY();
      mkPlayer.z = packet.getLocation().getZ();
      mkPlayer.packetStateData.lastClaimedPosition = packet.getLocation().getPosition();
    }

    if (packet.hasRotationChanged() && !ignoreRotation) {
      float newYaw = packet.getLocation().getYaw();
      float newPitch = packet.getLocation().getPitch();
      float deltaYaw = newYaw - mkPlayer.yaw;
      float deltaPitch = newPitch - mkPlayer.pitch;

      RotationUpdate update = mkPlayer.rotationUpdate;

      update.getFrom().setYaw(mkPlayer.yaw);
      update.getFrom().setPitch(mkPlayer.pitch);
      update.getTo().setYaw(newYaw);
      update.getTo().setPitch(newPitch);
      update.setDeltaYaw(deltaYaw);
      update.setDeltaPitch(deltaPitch);

      mkPlayer.getCheckManager().onRotationUpdate(update);

      mkPlayer.lastYaw = mkPlayer.yaw;
      mkPlayer.lastPitch = mkPlayer.pitch;
      mkPlayer.yaw = newYaw;
      mkPlayer.pitch = newPitch;
    }
  }

  private void updatePlayerState(MKPlayer mkPlayer, WrapperPlayClientPlayerFlying flying) {
    if (flying.hasPositionChanged()) {
      mkPlayer.x = flying.getLocation().getX();
      mkPlayer.y = flying.getLocation().getY();
      mkPlayer.z = flying.getLocation().getZ();
    }
    if (flying.hasRotationChanged()) {
      mkPlayer.yaw = flying.getLocation().getYaw();
      mkPlayer.pitch = flying.getLocation().getPitch();
    }
  }

  private void resetFlags(MKPlayer mkPlayer) {
    mkPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
    mkPlayer.packetStateData.lastPacketWasTeleport = false;
    mkPlayer.packetStateData.lastPacketWasServerRotation = false;
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }

    MKPlayer mkPlayer = playerDataManager.getPlayer((Player) event.getPlayer());
    if (mkPlayer == null) return;

    if (!(event.getPacketType() instanceof PacketType.Play.Server)) {
      return;
    }

    final PacketType.Play.Server packetType = (PacketType.Play.Server) event.getPacketType();

    if (packetType == PacketType.Play.Server.WINDOW_CONFIRMATION) {
      handleWindowConfirmation(new WrapperPlayServerWindowConfirmation(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.PING) {
      handlePing(new WrapperPlayServerPing(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_ENTITY) {
      handleSpawnEntity(new WrapperPlayServerSpawnEntity(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
      handleSpawnLivingEntity(new WrapperPlayServerSpawnLivingEntity(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_PAINTING) {
      handleSpawnPainting(new WrapperPlayServerSpawnPainting(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_PLAYER) {
      handleSpawnPlayer(new WrapperPlayServerSpawnPlayer(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.DESTROY_ENTITIES) {
      handleDestroyEntities(new WrapperPlayServerDestroyEntities(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.JOIN_GAME) {
      handleJoinGame(new WrapperPlayServerJoinGame(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.RESPAWN) {
      handleRespawn(mkPlayer);
    } else if (packetType == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
      handlePositionAndLook(new WrapperPlayServerPlayerPositionAndLook(event), mkPlayer);
    } else if (packetType == PacketType.Play.Server.PLAYER_ROTATION) {
      handlePlayerRotation(new WrapperPlayServerPlayerRotation(event), mkPlayer);
    }
  }

  private void handleWindowConfirmation(
      WrapperPlayServerWindowConfirmation confirmation, MKPlayer mkPlayer) {
    short id = confirmation.getActionId();
    if (id <= 0 && mkPlayer.didWeSendThatTrans.remove(id)) {
      mkPlayer.entitiesDespawnedThisTransaction.clear();
      mkPlayer.transactionsSent.add(new Pair<>(id, System.nanoTime()));
      mkPlayer.getLastTransactionSent().getAndIncrement();
    }
  }

  private void handlePing(WrapperPlayServerPing ping, MKPlayer mkPlayer) {
    int id = ping.getId();
    if (id == (short) id && mkPlayer.didWeSendThatTrans.remove((short) id)) {
      mkPlayer.entitiesDespawnedThisTransaction.clear();
      mkPlayer.transactionsSent.add(new Pair<>((short) id, System.nanoTime()));
      mkPlayer.getLastTransactionSent().getAndIncrement();
    }
  }

  private void handleSpawnEntity(WrapperPlayServerSpawnEntity spawn, MKPlayer mkPlayer) {
    if (mkPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      mkPlayer.sendTransaction();
    }
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () ->
                mkPlayer
                    .getCompensatedEntities()
                    .addEntity(
                        spawn.getEntityId(), spawn.getUUID().orElse(null), spawn.getEntityType()));
  }

  private void handleSpawnLivingEntity(
      WrapperPlayServerSpawnLivingEntity spawn, MKPlayer mkPlayer) {
    if (mkPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      mkPlayer.sendTransaction();
    }
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () ->
                mkPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getEntityUUID(), spawn.getEntityType()));
  }

  private void handleSpawnPainting(WrapperPlayServerSpawnPainting spawn, MKPlayer mkPlayer) {
    if (mkPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      mkPlayer.sendTransaction();
    }
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () ->
                mkPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getUUID(), EntityTypes.PAINTING));
  }

  private void handleSpawnPlayer(WrapperPlayServerSpawnPlayer spawn, MKPlayer mkPlayer) {
    if (mkPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      mkPlayer.sendTransaction();
    }
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () ->
                mkPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getUUID(), EntityTypes.PLAYER));
  }

  private void handleDestroyEntities(WrapperPlayServerDestroyEntities destroy, MKPlayer mkPlayer) {
    for (int id : destroy.getEntityIds()) {
      mkPlayer.entitiesDespawnedThisTransaction.add(id);
    }
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get() + 1,
            () -> {
              for (int id : destroy.getEntityIds()) {
                mkPlayer.getCompensatedEntities().removeEntity(id);
              }
            });
  }

  private void handleJoinGame(WrapperPlayServerJoinGame join, MKPlayer mkPlayer) {
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () -> {
              mkPlayer.setEntityId(join.getEntityId());
              mkPlayer.setGameMode(join.getGameMode());
              mkPlayer.getCompensatedEntities().clear();
            });
  }

  private void handleRespawn(MKPlayer mkPlayer) {
    mkPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            mkPlayer.getLastTransactionSent().get(),
            () -> mkPlayer.getCompensatedEntities().clear());
  }

  private void handlePositionAndLook(
      WrapperPlayServerPlayerPositionAndLook wrapper, MKPlayer mkPlayer) {
    mkPlayer.sendTransaction();
    int transactionId = mkPlayer.getLastTransactionSent().get();
    Vector3d location = new Vector3d(wrapper.getX(), wrapper.getY(), wrapper.getZ());
    RelativeFlag flags = wrapper.getRelativeFlags();
    mkPlayer.getPendingTeleports().add(new MKPlayer.TeleportData(location, flags, transactionId));
  }

  private void handlePlayerRotation(WrapperPlayServerPlayerRotation wrapper, MKPlayer mkPlayer) {
    mkPlayer.sendTransaction();
    int transactionId = mkPlayer.getLastTransactionSent().get();
    mkPlayer
        .getPendingRotations()
        .add(new MKPlayer.RotationData(wrapper.getYaw(), wrapper.getPitch(), transactionId));
  }

  private boolean addTransactionResponse(MKPlayer player, short id) {
    Pair<Short, Long> data = null;
    boolean hasID = false;

    for (Pair<Short, Long> iterator : player.transactionsSent) {
      if (iterator.first() == id) {
        hasID = true;
        break;
      }
    }

    if (hasID) {
      do {
        data = player.transactionsSent.poll();
        if (data == null) break;
        player.getLastTransactionReceived().incrementAndGet();
      } while (data.first() != id);

      player
          .getLatencyUtils()
          .handleNettySyncTransaction(player.getLastTransactionReceived().get());
    }
    return data != null;
  }

  private void isMojangStupid(
      MKPlayer player, WrapperPlayClientPlayerFlying flying, PacketReceiveEvent event) {
    if (player.packetStateData.lastPacketWasTeleport) return;
    if (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) return;

    final Location location = flying.getLocation();
    final double threshold = player.getMovementThreshold();
    final boolean inVehicle = player.getCompensatedEntities().self.getRiding() != null;

    if (!player.packetStateData.lastPacketWasTeleport
        && flying.hasPositionChanged()
        && flying.hasRotationChanged()
        && ((flying.isOnGround() == player.packetStateData.packetPlayerOnGround
                && (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)
                    && player.packetStateData.lastClaimedPosition.distanceSquared(
                            location.getPosition())
                        < threshold * threshold))
            || inVehicle)) {

      if (player.isCancelDuplicatePacket()) {
        event.setCancelled(true);
      }

      player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

      if (!player.packetStateData.ignoreDuplicatePacketRotation) {
        if (player.yaw != location.getYaw() || player.pitch != location.getPitch()) {
          player.lastYaw = player.yaw;
          player.lastPitch = player.pitch;
        }
        player.yaw = location.getYaw();
        player.pitch = location.getPitch();
      }

      player.packetStateData.lastClaimedPosition = location.getPosition();
    }
  }
}
