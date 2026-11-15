/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
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
package ru.korpys667.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.event.UserDisconnectEvent
import com.github.retrooper.packetevents.event.UserLoginEvent
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.protocol.potion.PotionType
import com.github.retrooper.packetevents.protocol.potion.PotionTypes
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPainting
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import java.util.Optional
import java.util.UUID
import org.bukkit.entity.Player
import ru.korpys667.checks.impl.misc.ClientBrand
import ru.korpys667.data.CollectManager
import ru.korpys667.data.TickData
import ru.korpys667.database.DatabaseManager
import ru.korpys667.debug.DebugCategory
import ru.korpys667.debug.DebugManager
import ru.korpys667.entity.PacketEntity
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.player.TransactionStamp
import ru.korpys667.player.state.TrackingState
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.utils.nmsutil.BlockFriction
import ru.korpys667.utils.update.RotationUpdate

private const val FULL_CIRCLE_DEGREES = 360f
private const val MAX_PITCH = 90f

@Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
class PacketListener(
  private val playerDataManager: PlayerDataManager,
  private val collectManager: CollectManager,
  private val debugManager: DebugManager,
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
) : PacketListenerAbstract() {
  override fun onUserLogin(event: UserLoginEvent) {
    val user: com.github.retrooper.packetevents.protocol.player.User? = event.user
    if (user == null) {
      return
    }
    val player: Player? = event.getPlayer()
    if (player == null) {
      return
    }
    playerDataManager.handleUserLogin(user, player)
  }

  override fun onUserDisconnect(event: UserDisconnectEvent) {
    playerDataManager.handleUserDisconnect(event.user)
  }

  private fun checkTeleportQueue(
    player: MKACPlayer,
    flying: WrapperPlayClientPlayerFlying,
  ): Boolean {
    if (
      !flying.hasPositionChanged() ||
        !flying.hasRotationChanged() ||
        player.pendingTeleports.isEmpty()
    ) {
      return false
    }

    val movement = player.movement
    while (player.pendingTeleports.isNotEmpty()) {
      val teleport = player.pendingTeleports.peek() ?: break
      val lastTransaction = player.transactions.lastTransactionReceived.get()
      if (lastTransaction < teleport.transactionId) {
        return false
      }

      val flyingLocation = flying.location
      val flags = teleport.flags

      val expectedX =
        if (flags.has(RelativeFlag.X)) {
          movement.x + teleport.location.x
        } else {
          teleport.location.x
        }
      val expectedY =
        if (flags.has(RelativeFlag.Y)) {
          movement.y + teleport.location.y
        } else {
          teleport.location.y
        }
      val expectedZ =
        if (flags.has(RelativeFlag.Z)) {
          movement.z + teleport.location.z
        } else {
          teleport.location.z
        }

      val threshold = if (teleport.isRelativePos()) player.getMovementThreshold() else 0.0
      val positionMatches =
        kotlin.math.abs(flyingLocation.x - expectedX) <= threshold &&
          kotlin.math.abs(flyingLocation.y - expectedY) <= TELEPORT_EPSILON + threshold &&
          kotlin.math.abs(flyingLocation.z - expectedZ) <= threshold
      if (positionMatches && teleport.rotationMatches(flyingLocation.yaw, flyingLocation.pitch)) {
        player.pendingTeleports.poll()
        return true
      }

      if (lastTransaction > teleport.transactionId) {
        player.pendingTeleports.poll()
        continue
      }
      return false
    }
    return false
  }

  private fun checkRotationQueue(
    player: MKACPlayer,
    flying: WrapperPlayClientPlayerFlying,
  ): Boolean {
    if (
      !flying.hasRotationChanged() ||
        flying.hasPositionChanged() ||
        player.pendingRotations.isEmpty()
    ) {
      return false
    }

    while (player.pendingRotations.isNotEmpty()) {
      val rotation = player.pendingRotations.peek() ?: break
      val lastTransaction = player.transactions.lastTransactionReceived.get()
      if (lastTransaction < rotation.transactionId) {
        return false
      }

      if (matchesServerRotation(rotation, flying)) {
        player.pendingRotations.poll()
        return true
      }

      if (lastTransaction > rotation.transactionId) {
        player.pendingRotations.poll()
        continue
      }
      return false
    }
    return false
  }

  override fun onPacketReceive(event: PacketReceiveEvent) {
    val mkacPlayer = playerDataManager.getPlayer(event.user) ?: return

    if (!mkacPlayer.isAttached) {
      mkacPlayer.checkManager.getCheck(ClientBrand::class.java)?.onPacketReceive(event)
      return
    }

    if (handleTransaction(event, mkacPlayer)) {
      dropWrapperUnlessRewritten(event)
      return
    }

    val isFlying = WrapperPlayClientPlayerFlying.isFlying(event.packetType)

    if (isFlying) {
      mkacPlayer.tracking.onTickStart()
    }

    handleMKACClientPackets(event, mkacPlayer)

    if (isFlying) {
      handleFlying(event, mkacPlayer)
    }

    if (event.isCancelled) {
      if (isFlying) {
        mkacPlayer.tracking.onTickAborted()
      }
      resetFlags(mkacPlayer)
      return
    }

    if (
      mkacPlayer.packetStateData.lastPacketWasTeleport ||
        mkacPlayer.packetStateData.lastPacketWasServerRotation
    ) {
      updatePlayerState(mkacPlayer, WrapperPlayClientPlayerFlying(event))
    }

    if (isFlying) {
      onDataTick(mkacPlayer)
    }

    mkacPlayer.checkManager.onPacketReceive(event)

    if (isFlying) {
      mkacPlayer.tracking.onTickEnd()
    }

    resetFlags(mkacPlayer)
    dropWrapperUnlessRewritten(event)
  }

  private fun dropWrapperUnlessRewritten(event: ProtocolPacketEvent) {
    if (!event.needsReEncode()) {
      event.setLastUsedWrapper(null)
    }
  }

  private fun handleTransaction(event: PacketReceiveEvent, mkacPlayer: MKACPlayer): Boolean {
    if (event.packetType == PacketType.Play.Client.WINDOW_CONFIRMATION) {
      val transaction = WrapperPlayClientWindowConfirmation(event)
      val id = transaction.actionId
      if (id <= 0 && addTransactionResponse(mkacPlayer, id)) {
        event.isCancelled = true
      }
      return true
    } else if (event.packetType == PacketType.Play.Client.PONG) {
      val id = WrapperPlayClientPong(event).id
      // a PONG id wider than a short is not ours
      if (id == id.toShort().toInt() && addTransactionResponse(mkacPlayer, id.toShort())) {
        event.isCancelled = true
      }
      return true
    }
    return false
  }

  private fun matchesServerRotation(
    rotation: MKACPlayer.RotationData,
    flying: WrapperPlayClientPlayerFlying,
  ): Boolean {
    val yawMatches = rotation.relativeYaw || flying.location.yaw == rotation.yaw
    val pitchMatches = rotation.relativePitch || flying.location.pitch == rotation.pitch
    return yawMatches && pitchMatches
  }

  private fun handleFlying(event: PacketReceiveEvent, mkacPlayer: MKACPlayer) {
    val flying = WrapperPlayClientPlayerFlying(event)

    val teleported = checkTeleportQueue(mkacPlayer, flying)
    val serverRotated = !teleported && checkRotationQueue(mkacPlayer, flying)

    mkacPlayer.packetStateData.lastPacketWasTeleport = teleported
    mkacPlayer.packetStateData.lastPacketWasServerRotation = serverRotated

    if (teleported || serverRotated) {
      mkacPlayer.tracking.firstTickProcessed = false
    }

    isMojangStupid(mkacPlayer, flying, event)

    if (!event.isCancelled) {
      if (!teleported && !serverRotated) {
        processRotation(mkacPlayer, flying)
      }
      updateBlockContext(mkacPlayer)
      if (!teleported && !mkacPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
        tickEntityInterpolation(mkacPlayer)
      }
    }

    if (!teleported) {
      mkacPlayer.packetStateData.packetPlayerOnGround = flying.isOnGround
    }
  }

  private fun tickEntityInterpolation(mkacPlayer: MKACPlayer) {
    // A tick without a movement packet leaves no trace, so any 1.9+ client can skip one unseen.
    val reliable = mkacPlayer.user.clientVersion.isOlderThan(ClientVersion.V_1_9)
    for (entity in mkacPlayer.compensatedEntities.entityMap.values) {
      entity.onMovement(tickingReliably = reliable)
    }
  }

  private fun onDataTick(mkacPlayer: MKACPlayer) {
    if (mkacPlayer.tracking.pendingBufferReset) {
      mkacPlayer.tracking.pendingBufferReset = false
      mkacPlayer.tickBuffer.resetForSession()
    }
    mkacPlayer.ensureTickBufferCapacity()
    if (
      mkacPlayer.packetStateData.shouldIgnoreFlyingTick && !mkacPlayer.tracking.windowStartThisTick
    ) {
      return
    }

    val withinFloodBudget = mkacPlayer.tracking.floodTryConsume(System.nanoTime())
    if (!withinFloodBudget && !mkacPlayer.tracking.windowStartThisTick) {
      mkacPlayer.tracking.floodDroppedCaptures++
      if (debugManager.isEnabled(DebugCategory.AI_FLOOD)) {
        debugManager.log(
          DebugCategory.AI_FLOOD,
          "Dropped flooded capture for ${mkacPlayer.player.name} " +
            "(dropped=${mkacPlayer.tracking.floodDroppedCaptures})",
        )
      }
      return
    }

    val buf = mkacPlayer.tickBuffer
    buf.capture(mkacPlayer)
    if (mkacPlayer.tracking.windowStartThisTick) {
      buf.markAttack()
    }
    collectManager.onTick(mkacPlayer)
    mkacPlayer.checkManager.onDataTick(mkacPlayer)
    buf.advance()
    mkacPlayer.compensatedFireworks.onTickEnd()
  }

  private fun processRotation(mkacPlayer: MKACPlayer, packet: WrapperPlayClientPlayerFlying) {
    val ignoreRotation =
      mkacPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate &&
        mkacPlayer.isIgnoreDuplicatePacketRotation()
    val movement = mkacPlayer.movement

    if (packet.hasPositionChanged()) {
      movement.x = packet.location.x
      movement.y = packet.location.y
      movement.z = packet.location.z
      if (!mkacPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
        mkacPlayer.packetStateData.duplicatePacketFilterPosition = packet.location.position
      }
    }

    if (packet.hasRotationChanged() && !ignoreRotation) {
      mkacPlayer.tracking.rotationThisTick = true
      val newYaw = packet.location.yaw
      val newPitch = packet.location.pitch
      val deltaYaw = newYaw - movement.yaw
      val deltaPitch = newPitch - movement.pitch

      val update: RotationUpdate = mkacPlayer.rotationUpdate

      update.from.yaw = movement.yaw
      update.from.pitch = movement.pitch
      update.to.yaw = newYaw
      update.to.pitch = newPitch
      update.deltaYaw = deltaYaw
      update.deltaPitch = deltaPitch

      mkacPlayer.checkManager.onRotationUpdate(update)

      movement.lastYaw = movement.yaw
      movement.lastPitch = movement.pitch
      movement.yaw = newYaw
      movement.pitch = newPitch
    }
  }

  private fun updatePlayerState(mkacPlayer: MKACPlayer, flying: WrapperPlayClientPlayerFlying) {
    val movement = mkacPlayer.movement
    if (flying.hasPositionChanged()) {
      movement.x = flying.location.x
      movement.y = flying.location.y
      movement.z = flying.location.z
      mkacPlayer.packetStateData.duplicatePacketFilterPosition = flying.location.position
    }
    if (flying.hasRotationChanged()) {
      movement.yaw = flying.location.yaw
      movement.pitch = flying.location.pitch
      movement.lastYaw = movement.yaw
      movement.lastPitch = movement.pitch
    }
  }

  private fun resetFlags(mkacPlayer: MKACPlayer) {
    mkacPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false
    mkacPlayer.packetStateData.lastPacketWasTeleport = false
    mkacPlayer.packetStateData.lastPacketWasServerRotation = false
  }

  override fun onPacketSend(event: PacketSendEvent) {
    if (event.packetType == PacketType.Login.Server.LOGIN_SUCCESS) {
      val user = event.user
      event.tasksAfterSend.add(Runnable { playerDataManager.handleUserConnect(user) })
      return
    }
    val mkacPlayer = playerDataManager.getPlayer(event.user)
    if (mkacPlayer != null) {
      handleMKACServerPackets(event, mkacPlayer)
      (event.packetType as? PacketType.Play.Server)?.let { packetType ->
        when (packetType) {
          PacketType.Play.Server.WINDOW_CONFIRMATION ->
            handleWindowConfirmation(WrapperPlayServerWindowConfirmation(event), mkacPlayer)
          PacketType.Play.Server.PING -> handlePing(WrapperPlayServerPing(event), mkacPlayer)
          PacketType.Play.Server.SPAWN_ENTITY ->
            handleSpawnEntity(WrapperPlayServerSpawnEntity(event), mkacPlayer)
          PacketType.Play.Server.SPAWN_LIVING_ENTITY ->
            handleSpawnLivingEntity(WrapperPlayServerSpawnLivingEntity(event), mkacPlayer)
          PacketType.Play.Server.SPAWN_PAINTING ->
            handleSpawnPainting(WrapperPlayServerSpawnPainting(event), mkacPlayer)
          PacketType.Play.Server.SPAWN_PLAYER ->
            handleSpawnPlayer(WrapperPlayServerSpawnPlayer(event), mkacPlayer)
          PacketType.Play.Server.SET_PASSENGERS ->
            handleSetPassengers(event, WrapperPlayServerSetPassengers(event), mkacPlayer)
          PacketType.Play.Server.ATTACH_ENTITY ->
            handleAttachEntity(event, WrapperPlayServerAttachEntity(event), mkacPlayer)
          PacketType.Play.Server.SET_SLOT ->
            handleSetSlot(WrapperPlayServerSetSlot(event), mkacPlayer)
          PacketType.Play.Server.WINDOW_ITEMS ->
            handleWindowItems(WrapperPlayServerWindowItems(event), mkacPlayer)
          PacketType.Play.Server.DESTROY_ENTITIES ->
            handleDestroyEntities(WrapperPlayServerDestroyEntities(event), mkacPlayer)
          PacketType.Play.Server.JOIN_GAME ->
            handleJoinGame(event, WrapperPlayServerJoinGame(event), mkacPlayer)
          PacketType.Play.Server.CHANGE_GAME_STATE ->
            handleChangeGameState(WrapperPlayServerChangeGameState(event), mkacPlayer)
          PacketType.Play.Server.RESPAWN ->
            handleRespawn(event, WrapperPlayServerRespawn(event), mkacPlayer)
          PacketType.Play.Server.PLAYER_POSITION_AND_LOOK ->
            handlePositionAndLook(event, WrapperPlayServerPlayerPositionAndLook(event), mkacPlayer)
          PacketType.Play.Server.PLAYER_ROTATION ->
            handlePlayerRotation(WrapperPlayServerPlayerRotation(event), mkacPlayer)
          PacketType.Play.Server.ENTITY_RELATIVE_MOVE -> {
            val move = WrapperPlayServerEntityRelativeMove(event)
            handleMoveEntity(
              mkacPlayer,
              move.entityId,
              move.deltaX,
              move.deltaY,
              move.deltaZ,
              relative = true,
              hasPos = true,
            )
          }
          PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION -> {
            val move = WrapperPlayServerEntityRelativeMoveAndRotation(event)
            handleMoveEntity(
              mkacPlayer,
              move.entityId,
              move.deltaX,
              move.deltaY,
              move.deltaZ,
              relative = true,
              hasPos = true,
            )
          }
          PacketType.Play.Server.ENTITY_TELEPORT -> {
            val tp = WrapperPlayServerEntityTeleport(event)
            handleMoveEntity(
              mkacPlayer,
              tp.entityId,
              tp.position.x,
              tp.position.y,
              tp.position.z,
              relative = false,
              hasPos = true,
            )
          }
          PacketType.Play.Server.ENTITY_ROTATION -> {
            val rotation = WrapperPlayServerEntityRotation(event)
            handleMoveEntity(
              mkacPlayer,
              rotation.entityId,
              0.0,
              0.0,
              0.0,
              relative = true,
              hasPos = false,
            )
          }
          PacketType.Play.Server.ENTITY_POSITION_SYNC -> {
            val sync = WrapperPlayServerEntityPositionSync(event)
            val pos = sync.values.position
            handleMoveEntity(
              mkacPlayer,
              sync.id,
              pos.x,
              pos.y,
              pos.z,
              relative = false,
              hasPos = true,
            )
          }
          else -> Unit
        }
      }
      dropWrapperUnlessRewritten(event)
    }
  }

  private fun handleMKACClientPackets(event: PacketReceiveEvent, mkacPlayer: MKACPlayer) {
    val tracking = mkacPlayer.tracking

    when (event.packetType) {
      PacketType.Play.Client.INTERACT_ENTITY -> {
        val interact = WrapperPlayClientInteractEntity(event)
        if (interact.action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
          handleAttack(interact.entityId, mkacPlayer)
        }
      }
      PacketType.Play.Client.ATTACK -> {
        handleAttack(WrapperPlayClientAttack(event).entityId, mkacPlayer)
      }
      PacketType.Play.Client.ANIMATION -> {
        if (WrapperPlayClientAnimation(event).hand == InteractionHand.MAIN_HAND) {
          tracking.onSwing()
        }
      }
      PacketType.Play.Client.USE_ITEM -> {
        tracking.ticksSinceUseItem = 0
      }
      PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> {
        tracking.ticksSinceUseItem = 0
        recordBlockPlacement(WrapperPlayClientPlayerBlockPlacement(event), mkacPlayer)
      }
      PacketType.Play.Client.PLAYER_DIGGING -> {
        val dig = WrapperPlayClientPlayerDigging(event)
        if (dig.action == DiggingAction.RELEASE_USE_ITEM) {
          tracking.isUsingItem = false
          tracking.updateActiveItem()
        }
        if (dig.action == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
          tracking.swapOffhand()
          updateHeldAttackSpeed(mkacPlayer)
        }
      }
      PacketType.Play.Client.HELD_ITEM_CHANGE -> {
        tracking.isUsingItem = false
        tracking.updateActiveItem()
        tracking.cooldownTicks = 0
        tracking.heldSlot = WrapperPlayClientHeldItemChange(event).slot
        tracking.onSlotSwitch()
        updateHeldAttackSpeed(mkacPlayer)
      }
      PacketType.Play.Client.ENTITY_ACTION -> {
        val action = WrapperPlayClientEntityAction(event)
        when (action.action) {
          WrapperPlayClientEntityAction.Action.START_SPRINTING -> {
            tracking.sprinting = true
          }
          WrapperPlayClientEntityAction.Action.STOP_SPRINTING -> {
            tracking.sprinting = false
          }
          WrapperPlayClientEntityAction.Action.START_SNEAKING -> tracking.sneaking = true
          WrapperPlayClientEntityAction.Action.STOP_SNEAKING -> tracking.sneaking = false
          WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA -> tracking.gliding = true
          else -> Unit
        }
      }
      PacketType.Play.Client.PLAYER_ABILITIES -> {
        tracking.flying = WrapperPlayClientPlayerAbilities(event).isFlying
      }
      PacketType.Play.Client.PLAYER_INPUT -> {
        val input = WrapperPlayClientPlayerInput(event)
        tracking.inputForward = input.isForward
        tracking.inputBackward = input.isBackward
        tracking.inputLeft = input.isLeft
        tracking.inputRight = input.isRight
        tracking.inputJump = input.isJump
        tracking.inputShift = input.isShift
        tracking.inputSprint = input.isSprint
        // Clients below 1.21.2 cannot send this packet, so a proxy wrote the values.
        tracking.inputValid =
          mkacPlayer.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21_2)
        if (mkacPlayer.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21_6)) {
          tracking.sneaking = input.isShift
        }
      }
      PacketType.Play.Client.STEER_VEHICLE -> {
        val steer = WrapperPlayClientSteerVehicle(event)
        tracking.inputForward = steer.forward > 0f
        tracking.inputBackward = steer.forward < 0f
        tracking.inputLeft = steer.sideways > 0f
        tracking.inputRight = steer.sideways < 0f
        tracking.inputJump = steer.isJump
        tracking.inputShift = steer.isUnmount
        tracking.inputValid = true
      }
      PacketType.Play.Client.VEHICLE_MOVE -> {
        if (tracking.inVehicle) {
          val position = WrapperPlayClientVehicleMove(event).position
          val movement = mkacPlayer.movement
          movement.x = position.x
          movement.y = position.y
          movement.z = position.z
        }
      }
      else -> Unit
    }
  }

  private fun recordBlockPlacement(
    placement: WrapperPlayClientPlayerBlockPlacement,
    mkacPlayer: MKACPlayer,
  ) {
    val pos = placement.blockPosition
    val tracking = mkacPlayer.tracking
    val state = mkacPlayer.compensatedWorld.getBlock(pos.x, pos.y, pos.z)
    val blockClass =
      when (state?.type) {
        null -> TickData.PLACE_UNKNOWN
        StateTypes.OBSIDIAN,
        StateTypes.CRYING_OBSIDIAN -> TickData.PLACE_OBSIDIAN
        StateTypes.BEDROCK -> TickData.PLACE_BEDROCK
        StateTypes.RESPAWN_ANCHOR -> TickData.PLACE_RESPAWN_ANCHOR
        else -> TickData.PLACE_OTHER_SOLID
      }
    val cursor = placement.cursorPosition
    tracking.onBlockPlace(
      placement.faceId,
      blockClass.toInt(),
      pos.x,
      pos.y,
      pos.z,
      floatArrayOf(cursor.x, cursor.y, cursor.z),
      placement.insideBlock.orElse(false),
      placement.hand == InteractionHand.OFF_HAND,
    )
    if (state?.type == StateTypes.RESPAWN_ANCHOR) {
      tracking.anchorCharge = state.charges
      tracking.anchorUseInterval =
        mkacPlayer.crystalTracker.anchorUseInterval(pos.x, pos.y, pos.z, tracking.tickIndex)
      tracking.raiseWindowStart(TickData.START_USE_RESPAWN_ANCHOR)
    } else if (blockClass == TickData.PLACE_OBSIDIAN || blockClass == TickData.PLACE_BEDROCK) {
      val held = tracking.hotbarItems.getOrNull(tracking.heldSlot)
      if (held == ItemTypes.END_CRYSTAL) tracking.raiseWindowStart(TickData.START_PLACE_END_CRYSTAL)
    }
  }

  private fun recordFirstAttack(mkacPlayer: MKACPlayer) {
    if (mkacPlayer.combat.hasAttacked) return
    mkacPlayer.combat.hasAttacked = true
    val attacker = mkacPlayer.uuid
    val now = System.currentTimeMillis()
    scheduler.runAsync { databaseManager.database.recordAttack(attacker, now) }
  }

  private fun handleAttack(targetId: Int, mkacPlayer: MKACPlayer) {
    if (targetId == mkacPlayer.entityId) return
    val target = mkacPlayer.compensatedEntities.getEntity(targetId)
    val tracking = mkacPlayer.tracking
    tracking.crystalSpawnToAttack =
      mkacPlayer.crystalTracker.spawnToAttack(targetId, tracking.tickIndex)
    tracking.attackTargetType = classifyAttackTarget(target)
    if (target != null && target.isPlayer) {
      tracking.onAttack(targetId, tracking.sprinting)
      tracking.raiseWindowStart(TickData.START_MELEE_PLAYER)
      recordFirstAttack(mkacPlayer)
      return
    }
    tracking.raiseWindowStart(
      when {
        target == null -> TickData.START_ATTACK_ENTITY_OTHER
        target.type == EntityTypes.END_CRYSTAL -> TickData.START_ATTACK_END_CRYSTAL
        target.isLivingEntity -> TickData.START_MELEE_LIVING_OTHER
        else -> TickData.START_ATTACK_ENTITY_OTHER
      }
    )
  }

  private fun classifyAttackTarget(target: PacketEntity?): Short =
    when {
      target == null -> TickData.TARGET_UNKNOWN
      target.isPlayer -> TickData.TARGET_PLAYER
      target.type == EntityTypes.END_CRYSTAL -> TickData.TARGET_END_CRYSTAL
      target.isLivingEntity -> TickData.TARGET_LIVING_OTHER
      EntityTypes.isTypeInstanceOf(target.type, EntityTypes.BOAT) ||
        EntityTypes.isTypeInstanceOf(target.type, EntityTypes.MINECART) -> TickData.TARGET_VEHICLE
      else -> TickData.TARGET_OTHER
    }

  private fun handleMKACServerPackets(event: PacketSendEvent, mkacPlayer: MKACPlayer) {
    val tracking = mkacPlayer.tracking

    when (event.packetType) {
      PacketType.Play.Server.ENTITY_VELOCITY -> {
        val vel = WrapperPlayServerEntityVelocity(event)
        if (vel.entityId == mkacPlayer.entityId) {
          val velocity = vel.velocity
          mkacPlayer.sendTransaction()
          mkacPlayer.latencyUtils.addRealTimeTask(
            mkacPlayer.transactions.lastTransactionSent.get()
          ) {
            tracking.onKnockback(velocity.x, velocity.y, velocity.z)
          }
          event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
        }
      }
      PacketType.Play.Server.EXPLOSION -> {
        val explosion = WrapperPlayServerExplosion(event)
        val kb = explosion.playerMotion
        if (kb != null && (kb.x != 0f || kb.y != 0f || kb.z != 0f)) {
          mkacPlayer.sendTransaction()
          mkacPlayer.latencyUtils.addRealTimeTask(
            mkacPlayer.transactions.lastTransactionSent.get()
          ) {
            tracking.onExplosion(kb.x, kb.y, kb.z)
            tracking.raiseWindowStart(TickData.START_EXPLOSION_RECEIVED)
          }
          event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
        }
      }
      PacketType.Play.Server.ENTITY_EFFECT -> {
        val effect = WrapperPlayServerEntityEffect(event)
        if (effect.entityId == mkacPlayer.entityId) {
          val potionType = effect.potionType
          val amplifier = effect.effectAmplifier
          mkacPlayer.sendTransaction()
          mkacPlayer.latencyUtils.addRealTimeTask(
            mkacPlayer.transactions.lastTransactionSent.get()
          ) {
            handlePotionEffect(tracking, potionType, amplifier)
          }
          event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
        }
      }
      PacketType.Play.Server.REMOVE_ENTITY_EFFECT -> {
        val remove = WrapperPlayServerRemoveEntityEffect(event)
        if (remove.entityId == mkacPlayer.entityId) {
          val potionType = remove.potionType
          mkacPlayer.sendTransaction()
          mkacPlayer.latencyUtils.addRealTimeTask(
            mkacPlayer.transactions.lastTransactionSent.get()
          ) {
            handlePotionRemove(tracking, potionType)
          }
          event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
        }
      }
      PacketType.Play.Server.UPDATE_ATTRIBUTES -> {
        val attrs = WrapperPlayServerUpdateAttributes(event)
        val entityId = attrs.entityId
        val properties = attrs.properties
        val isSelf = entityId == mkacPlayer.entityId
        if (isSelf) mkacPlayer.sendTransaction()
        mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
          mkacPlayer.compensatedEntities.updateAttributes(entityId, properties)
        }
        if (isSelf) event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
      }
      PacketType.Play.Server.PLAYER_ABILITIES -> {
        val abilities = WrapperPlayServerPlayerAbilities(event)
        val isFlying = abilities.isFlying
        mkacPlayer.sendTransaction()
        mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
          tracking.flying = isFlying
        }
        event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
      }
      PacketType.Play.Server.UPDATE_HEALTH -> {
        val update = WrapperPlayServerUpdateHealth(event)
        val food = update.food
        val newHealth = update.health
        mkacPlayer.sendTransaction()
        mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
          val lost = tracking.health - newHealth
          if (lost > 0f) {
            tracking.damageTakenThisTick = lost
            tracking.ticksSinceDamage = 0
          }
          tracking.health = newHealth
          tracking.foodLevel = food
        }
        event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
      }
      PacketType.Play.Server.ENTITY_METADATA -> {
        handleEntityMetadata(WrapperPlayServerEntityMetadata(event), mkacPlayer)
      }
      PacketType.Play.Server.ENTITY_STATUS -> {
        val status = WrapperPlayServerEntityStatus(event)
        if (status.status == DEATH_ANIMATION_STATUS) {
          val entityId = status.entityId
          mkacPlayer.sendTransaction()
          mkacPlayer.latencyUtils.addRealTimeTask(
            mkacPlayer.transactions.lastTransactionSent.get()
          ) {
            mkacPlayer.compensatedEntities.getEntity(entityId)?.isDead = true
          }
          event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
        }
      }
      PacketType.Play.Server.CHUNK_DATA -> {
        handleChunkData(WrapperPlayServerChunkData(event), mkacPlayer)
      }
      PacketType.Play.Server.BLOCK_CHANGE -> {
        handleBlockChange(WrapperPlayServerBlockChange(event), mkacPlayer)
      }
      PacketType.Play.Server.MULTI_BLOCK_CHANGE -> {
        handleMultiBlockChange(WrapperPlayServerMultiBlockChange(event), mkacPlayer)
      }
      PacketType.Play.Server.UNLOAD_CHUNK -> {
        val unload = WrapperPlayServerUnloadChunk(event)
        mkacPlayer.compensatedWorld.onChunkUnload(unload.chunkX, unload.chunkZ)
      }
      else -> Unit
    }
  }

  private fun handlePotionEffect(tracking: TrackingState, potionType: PotionType?, amplifier: Int) {
    if (potionType == null) return
    when (potionType) {
      PotionTypes.JUMP_BOOST -> tracking.jumpAmplifier = amplifier
      PotionTypes.SLOW_FALLING -> tracking.slowFalling = true
      PotionTypes.BLINDNESS -> tracking.hasBlindness = true
      PotionTypes.SPEED -> tracking.speedAmplifier = amplifier
      PotionTypes.SLOWNESS -> tracking.slownessAmplifier = amplifier
      PotionTypes.HASTE -> tracking.hasteAmplifier = amplifier
      PotionTypes.MINING_FATIGUE -> tracking.miningFatigueAmplifier = amplifier
      PotionTypes.LEVITATION -> tracking.levitationAmplifier = amplifier
      PotionTypes.DOLPHINS_GRACE -> tracking.dolphinsGrace = true
      else -> Unit
    }
  }

  private fun handlePotionRemove(tracking: TrackingState, potionType: PotionType?) {
    if (potionType == null) return
    when (potionType) {
      PotionTypes.JUMP_BOOST -> tracking.jumpAmplifier = -1
      PotionTypes.SLOW_FALLING -> tracking.slowFalling = false
      PotionTypes.BLINDNESS -> tracking.hasBlindness = false
      PotionTypes.SPEED -> tracking.speedAmplifier = -1
      PotionTypes.SLOWNESS -> tracking.slownessAmplifier = -1
      PotionTypes.HASTE -> tracking.hasteAmplifier = -1
      PotionTypes.MINING_FATIGUE -> tracking.miningFatigueAmplifier = -1
      PotionTypes.LEVITATION -> tracking.levitationAmplifier = -1
      PotionTypes.DOLPHINS_GRACE -> tracking.dolphinsGrace = false
      else -> Unit
    }
  }

  private fun handleEntityMetadata(
    meta: WrapperPlayServerEntityMetadata,
    mkacPlayer: MKACPlayer,
  ) {
    val entityId = meta.entityId
    val isSelf = entityId == mkacPlayer.entityId
    val metadata = meta.entityMetadata
    val serverVersion = PacketEvents.getAPI().serverManager.version
    val livingFlagsIndex =
      when {
        serverVersion.isNewerThanOrEquals(ServerVersion.V_1_17) -> LIVING_FLAGS_INDEX_MODERN
        serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14) -> LIVING_FLAGS_INDEX_POSE
        serverVersion.isNewerThanOrEquals(ServerVersion.V_1_10) -> LIVING_FLAGS_INDEX_LEGACY
        else -> LIVING_FLAGS_INDEX_ANCIENT
      }

    mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
      for (data in metadata) {
        handleEntityMetadataEntry(data, isSelf, livingFlagsIndex, entityId, mkacPlayer)
      }
    }
  }

  private fun handleEntityMetadataEntry(
    data: EntityData<*>,
    isSelf: Boolean,
    livingFlagsIndex: Int,
    entityId: Int,
    mkacPlayer: MKACPlayer,
  ) {
    when {
      data.index == 0 ->
        if (isSelf && data.value is Byte) {
          val flags = (data.value as Byte).toInt()
          mkacPlayer.tracking.gliding =
            (flags and GLIDING_FLAG) != 0 &&
              mkacPlayer.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_9)
          mkacPlayer.tracking.swimming =
            (flags and SWIMMING_FLAG) != 0 &&
              mkacPlayer.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13)
        }
      data.index == livingFlagsIndex ->
        if (isSelf && data.value is Byte) {
          val livingFlags = (data.value as Byte).toInt()
          mkacPlayer.tracking.riptideActive = (livingFlags and RIPTIDE_FLAG) == RIPTIDE_FLAG
          mkacPlayer.tracking.isUsingItem = (livingFlags and USE_ITEM_FLAG) != 0
          mkacPlayer.tracking.usingOffhand = (livingFlags and OFFHAND_FLAG) != 0
          mkacPlayer.tracking.updateActiveItem()
        }
      else ->
        if (!isSelf) {
          val entity = mkacPlayer.compensatedEntities.getEntity(entityId)
          if (entity != null) {
            handleTrackedEntityMetadata(entity, data)
            handleFireworkMetadata(entity, entityId, data, mkacPlayer)
          }
        }
    }
  }

  private fun handleTrackedEntityMetadata(entity: PacketEntity, data: EntityData<*>) {
    val value = data.value
    val type = entity.type
    when {
      value is EntityPose && type == EntityTypes.PLAYER -> entity.pose = value.ordinal
      value is Boolean && data.index in AGEABLE_BABY_INDEX_RANGE && isAgeableMob(type) ->
        entity.isBaby = value
      value is Int &&
        data.index >= SIZE_METADATA_MIN_INDEX &&
        (type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE) -> entity.slimeSize = value
      value is Int && data.index >= SIZE_METADATA_MIN_INDEX && type == EntityTypes.PHANTOM ->
        entity.phantomSize = value
    }
  }

  private fun handleFireworkMetadata(
    entity: PacketEntity,
    entityId: Int,
    data: EntityData<*>,
    mkacPlayer: MKACPlayer,
  ) {
    if (entity.type != EntityTypes.FIREWORK_ROCKET) return
    val serverVersion = PacketEvents.getAPI().serverManager.version
    val offset =
      when {
        serverVersion.isOlderThanOrEquals(ServerVersion.V_1_12_2) -> 2
        serverVersion.isOlderThanOrEquals(ServerVersion.V_1_16_5) -> 1
        else -> 0
      }
    if (data.index != FIREWORK_ATTACHED_INDEX_MODERN - offset) return

    val value = data.value
    val attachedId: Int? =
      when (value) {
        is Int -> value
        is Optional<*> -> value.orElse(null) as? Int
        else -> null
      }
    if (attachedId != null && attachedId == mkacPlayer.entityId) {
      mkacPlayer.compensatedFireworks.addNewFirework(entityId)
    }
  }

  private fun isAgeableMob(type: EntityType): Boolean {
    return type == EntityTypes.ZOMBIE ||
      type == EntityTypes.ZOMBIE_VILLAGER ||
      type == EntityTypes.HUSK ||
      type == EntityTypes.DROWNED ||
      type == EntityTypes.PIGLIN ||
      type == EntityTypes.ZOGLIN ||
      type == EntityTypes.PIG ||
      type == EntityTypes.COW ||
      type == EntityTypes.CHICKEN ||
      type == EntityTypes.SHEEP ||
      type == EntityTypes.VILLAGER ||
      type == EntityTypes.HORSE ||
      type == EntityTypes.DONKEY ||
      type == EntityTypes.MULE ||
      type == EntityTypes.LLAMA ||
      type == EntityTypes.MOOSHROOM ||
      type == EntityTypes.WOLF ||
      type == EntityTypes.CAT ||
      type == EntityTypes.OCELOT ||
      type == EntityTypes.RABBIT ||
      type == EntityTypes.POLAR_BEAR ||
      type == EntityTypes.TURTLE ||
      type == EntityTypes.PANDA ||
      type == EntityTypes.FOX ||
      type == EntityTypes.BEE ||
      type == EntityTypes.HOGLIN ||
      type == EntityTypes.STRIDER ||
      type == EntityTypes.GOAT ||
      type == EntityTypes.AXOLOTL ||
      type == EntityTypes.FROG ||
      type == EntityTypes.CAMEL ||
      type == EntityTypes.SNIFFER ||
      type == EntityTypes.ARMADILLO
  }

  private fun handleChunkData(chunk: WrapperPlayServerChunkData, mkacPlayer: MKACPlayer) {
    val col = chunk.column
    val sections = col.chunks ?: return
    mkacPlayer.compensatedWorld.onChunkLoad(col.x, col.z, sections)
  }

  private fun handleBlockChange(
    blockChange: WrapperPlayServerBlockChange,
    mkacPlayer: MKACPlayer,
  ) {
    val pos = blockChange.blockPosition
    val state = blockChange.blockState
    mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
      mkacPlayer.compensatedWorld.setBlock(pos.x, pos.y, pos.z, state)
    }
  }

  private fun handleMultiBlockChange(
    multi: WrapperPlayServerMultiBlockChange,
    mkacPlayer: MKACPlayer,
  ) {
    val version = mkacPlayer.user.clientVersion
    val changes = multi.blocks.map { Triple(it.x, it.y, it.z) to it.getBlockState(version) }
    mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
      for ((at, state) in changes) {
        mkacPlayer.compensatedWorld.setBlock(at.first, at.second, at.third, state)
      }
    }
  }

  @Suppress("MagicNumber")
  private fun itemAttackSpeed(item: ItemType?): Float =
    when (item) {
      ItemTypes.WOODEN_SWORD,
      ItemTypes.STONE_SWORD,
      ItemTypes.IRON_SWORD,
      ItemTypes.GOLDEN_SWORD,
      ItemTypes.DIAMOND_SWORD,
      ItemTypes.NETHERITE_SWORD -> 1.6f
      ItemTypes.WOODEN_AXE,
      ItemTypes.STONE_AXE -> 0.8f
      ItemTypes.IRON_AXE -> 0.9f
      ItemTypes.GOLDEN_AXE,
      ItemTypes.DIAMOND_AXE,
      ItemTypes.NETHERITE_AXE -> 1.0f
      ItemTypes.WOODEN_PICKAXE,
      ItemTypes.STONE_PICKAXE,
      ItemTypes.IRON_PICKAXE,
      ItemTypes.GOLDEN_PICKAXE,
      ItemTypes.DIAMOND_PICKAXE,
      ItemTypes.NETHERITE_PICKAXE -> 1.2f
      ItemTypes.WOODEN_SHOVEL,
      ItemTypes.STONE_SHOVEL,
      ItemTypes.IRON_SHOVEL,
      ItemTypes.GOLDEN_SHOVEL,
      ItemTypes.DIAMOND_SHOVEL,
      ItemTypes.NETHERITE_SHOVEL -> 1.0f
      ItemTypes.TRIDENT -> 1.1f
      else -> DEFAULT_ATTACK_SPEED
    }

  private fun updateHeldAttackSpeed(mkacPlayer: MKACPlayer) {
    val tracking = mkacPlayer.tracking
    tracking.attackSpeed = itemAttackSpeed(tracking.hotbarItems.getOrNull(tracking.heldSlot))
  }

  private fun handleSetSlot(wrapper: WrapperPlayServerSetSlot, mkacPlayer: MKACPlayer) {
    if (wrapper.windowId != 0) return
    if (wrapper.slot == OFFHAND_SLOT) {
      mkacPlayer.tracking.offhandItem = wrapper.item.type
      mkacPlayer.tracking.updateActiveItem()
      return
    }
    val hotbarIndex = wrapper.slot - HOTBAR_START_SLOT
    if (hotbarIndex in 0 until TrackingState.HOTBAR_SIZE) {
      mkacPlayer.tracking.hotbarItems[hotbarIndex] = wrapper.item.type
      mkacPlayer.tracking.updateActiveItem()
      updateHeldAttackSpeed(mkacPlayer)
    }
  }

  private fun handleWindowItems(wrapper: WrapperPlayServerWindowItems, mkacPlayer: MKACPlayer) {
    if (wrapper.windowId != 0) return
    val items = wrapper.items
    val tracking = mkacPlayer.tracking
    for (i in 0 until TrackingState.HOTBAR_SIZE) {
      val slot = HOTBAR_START_SLOT + i
      if (slot < items.size) tracking.hotbarItems[i] = items[slot].type
    }
    if (OFFHAND_SLOT < items.size) tracking.offhandItem = items[OFFHAND_SLOT].type
    tracking.updateActiveItem()
    updateHeldAttackSpeed(mkacPlayer)
  }

  private fun mostStuck(current: Float, candidate: Float): Float =
    if (kotlin.math.abs(candidate - 1f) > kotlin.math.abs(current - 1f)) candidate else current

  private fun computePose(tracking: TrackingState): Int =
    when {
      tracking.gliding -> EntityPose.FALL_FLYING.ordinal
      tracking.swimming -> EntityPose.SWIMMING.ordinal
      tracking.riptideActive -> EntityPose.SPIN_ATTACK.ordinal
      tracking.sneaking -> EntityPose.CROUCHING.ordinal
      else -> EntityPose.STANDING.ordinal
    }

  private fun handleChangeGameState(
    gs: WrapperPlayServerChangeGameState,
    mkacPlayer: MKACPlayer,
  ) {
    if (gs.reason == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
      mkacPlayer.tracking.gameMode = gs.value.toInt()
    }
  }

  private fun updateBlockContext(mkacPlayer: MKACPlayer) {
    val tracking = mkacPlayer.tracking
    val m = mkacPlayer.movement
    val world = mkacPlayer.compensatedWorld

    val footX = kotlin.math.floor(m.x).toInt()
    val footZ = kotlin.math.floor(m.z).toInt()
    val frictionY = kotlin.math.floor(m.y - GROUND_SEARCH_OFFSET).toInt()
    val feetY = kotlin.math.floor(m.y).toInt()
    val bodyY = kotlin.math.floor(m.y + BODY_OFFSET).toInt()
    val headY = kotlin.math.floor(m.y + HEAD_OFFSET).toInt()

    val frictionBlock = world.getBlock(footX, frictionY, footZ)
    if (frictionBlock != null) {
      val type = frictionBlock.type
      tracking.groundFriction = BlockFriction.of(type)
      tracking.onSlime = BlockFriction.isSlime(type)
      tracking.onHoney = BlockFriction.isHoney(type)
      tracking.onSoulSand = BlockFriction.isSoulSand(type)
      tracking.onMud = BlockFriction.isMud(type)
    } else {
      tracking.onSlime = false
      tracking.onHoney = false
      tracking.onSoulSand = false
      tracking.onMud = false
    }
    world.getBlock(footX, feetY, footZ)?.let { state ->
      tracking.isClimbing = BlockFriction.isClimbable(state.type)
      tracking.inWater = BlockFriction.isWater(state)
    }
    tracking.pose = computePose(tracking)

    var stuckX = 1.0f
    var stuckY = 1.0f
    var stuckZ = 1.0f
    for (y in intArrayOf(feetY, bodyY, headY)) {
      val state = world.getBlock(footX, y, footZ) ?: continue
      val stuck = BlockFriction.stuckMultiplier(state.type) ?: continue
      stuckX = mostStuck(stuckX, stuck[0])
      stuckY = mostStuck(stuckY, stuck[1])
      stuckZ = mostStuck(stuckZ, stuck[2])
    }
    tracking.stuckMultX = stuckX
    tracking.stuckMultY = stuckY
    tracking.stuckMultZ = stuckZ
  }

  @Suppress("LongParameterList")
  private fun handleMoveEntity(
    mkacPlayer: MKACPlayer,
    entityId: Int,
    deltaX: Double,
    deltaY: Double,
    deltaZ: Double,
    relative: Boolean,
    hasPos: Boolean,
  ) {
    val entity = mkacPlayer.compensatedEntities.getEntity(entityId)
    if (entity != null) {
      if (entity.lastTransactionHung == mkacPlayer.transactions.lastTransactionSent.get()) {
        mkacPlayer.sendTransaction()
      }
      entity.lastTransactionHung = mkacPlayer.transactions.lastTransactionSent.get()
    }

    val lastTrans = mkacPlayer.transactions.lastTransactionSent.get()
    mkacPlayer.latencyUtils.addRealTimeTask(lastTrans) {
      val ent = mkacPlayer.compensatedEntities.getEntity(entityId) ?: return@addRealTimeTask
      ent.onFirstTransaction(relative, hasPos, deltaX, deltaY, deltaZ, mkacPlayer)
    }

    mkacPlayer.latencyUtils.addRealTimeTask(lastTrans + 1) {
      val ent = mkacPlayer.compensatedEntities.getEntity(entityId) ?: return@addRealTimeTask
      ent.onSecondTransaction()
    }
  }

  private fun handleWindowConfirmation(
    confirmation: WrapperPlayServerWindowConfirmation,
    mkacPlayer: MKACPlayer,
  ) {
    val id = confirmation.actionId
    val transactions = mkacPlayer.transactions
    if (id <= 0 && transactions.didWeSendThatTrans.remove(id)) {
      transactions.entitiesDespawnedThisTransaction.clear()
      transactions.transactionsSent.add(TransactionStamp(id, System.nanoTime()))
      transactions.lastTransactionSent.getAndIncrement()
    }
  }

  private fun handlePing(ping: WrapperPlayServerPing, mkacPlayer: MKACPlayer) {
    val id = ping.id
    val transactions = mkacPlayer.transactions
    if (id == id.toShort().toInt() && transactions.didWeSendThatTrans.remove(id.toShort())) {
      transactions.entitiesDespawnedThisTransaction.clear()
      transactions.transactionsSent.add(TransactionStamp(id.toShort(), System.nanoTime()))
      transactions.lastTransactionSent.getAndIncrement()
    }
  }

  private fun handleSpawnEntity(spawn: WrapperPlayServerSpawnEntity, mkacPlayer: MKACPlayer) {
    if (mkacPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      mkacPlayer.sendTransaction()
    }
    val entityId = spawn.entityId
    val uuid = spawn.uuid.orElse(UUID(0, entityId.toLong()))
    val type = spawn.entityType
    val pos = spawn.position
    mkacPlayer.latencyUtils.addRealTimeTask(
      mkacPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        mkacPlayer.compensatedEntities.addEntity(entityId, uuid, type, pos.x, pos.y, pos.z)
        if (type == EntityTypes.END_CRYSTAL) {
          mkacPlayer.crystalTracker.onCrystalSpawn(entityId, mkacPlayer.tracking.tickIndex)
        }
      },
    )
  }

  private fun handleSpawnLivingEntity(
    spawn: WrapperPlayServerSpawnLivingEntity,
    mkacPlayer: MKACPlayer,
  ) {
    if (mkacPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      mkacPlayer.sendTransaction()
    }
    val entityId = spawn.entityId
    val uuid = spawn.entityUUID
    val type = spawn.entityType
    val pos = spawn.position
    mkacPlayer.latencyUtils.addRealTimeTask(
      mkacPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        mkacPlayer.compensatedEntities.addEntity(entityId, uuid, type, pos.x, pos.y, pos.z)
      },
    )
  }

  private fun handleSpawnPainting(spawn: WrapperPlayServerSpawnPainting, mkacPlayer: MKACPlayer) {
    if (mkacPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      mkacPlayer.sendTransaction()
    }
    val entityId = spawn.entityId
    val uuid = spawn.uuid
    mkacPlayer.latencyUtils.addRealTimeTask(
      mkacPlayer.transactions.lastTransactionSent.get(),
      Runnable { mkacPlayer.compensatedEntities.addEntity(entityId, uuid, EntityTypes.PAINTING) },
    )
  }

  private fun handleSpawnPlayer(spawn: WrapperPlayServerSpawnPlayer, mkacPlayer: MKACPlayer) {
    if (mkacPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      mkacPlayer.sendTransaction()
    }
    val entityId = spawn.entityId
    val uuid = spawn.uuid
    val pos = spawn.position
    mkacPlayer.latencyUtils.addRealTimeTask(
      mkacPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        mkacPlayer.compensatedEntities.addEntity(
          entityId,
          uuid,
          EntityTypes.PLAYER,
          pos.x,
          pos.y,
          pos.z,
        )
      },
    )
  }

  private fun handleSetPassengers(
    event: PacketSendEvent,
    wrapper: WrapperPlayServerSetPassengers,
    mkacPlayer: MKACPlayer,
  ) {
    val vehicleId = wrapper.entityId
    val isPassenger = wrapper.passengers.contains(mkacPlayer.entityId)

    mkacPlayer.sendTransaction()
    mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
      val self = mkacPlayer.compensatedEntities.self
      val vehicle = mkacPlayer.compensatedEntities.getEntity(vehicleId) ?: return@addRealTimeTask
      if (isPassenger) {
        self.mount(vehicle)
        mkacPlayer.tracking.inVehicle = true
      } else if (self.riding === vehicle) {
        self.eject()
        mkacPlayer.tracking.inVehicle = self.riding != null
      }
    }
    event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
  }

  private fun handleAttachEntity(
    event: PacketSendEvent,
    wrapper: WrapperPlayServerAttachEntity,
    mkacPlayer: MKACPlayer,
  ) {
    if (wrapper.isLeash || wrapper.attachedId != mkacPlayer.entityId) return
    val holdingId = wrapper.holdingId
    mkacPlayer.sendTransaction()
    mkacPlayer.latencyUtils.addRealTimeTask(mkacPlayer.transactions.lastTransactionSent.get()) {
      val self = mkacPlayer.compensatedEntities.self
      if (holdingId != -1) {
        val vehicle = mkacPlayer.compensatedEntities.getEntity(holdingId) ?: return@addRealTimeTask
        self.mount(vehicle)
        mkacPlayer.tracking.inVehicle = true
      } else if (self.riding != null) {
        self.eject()
        mkacPlayer.tracking.inVehicle = self.riding != null
      }
    }
    event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
  }

  private fun handleDestroyEntities(
    destroy: WrapperPlayServerDestroyEntities,
    mkacPlayer: MKACPlayer,
  ) {
    val entityIds = destroy.entityIds
    for (id in entityIds) {
      mkacPlayer.transactions.entitiesDespawnedThisTransaction.add(id)
    }
    val destroyTransaction = mkacPlayer.transactions.lastTransactionSent.get()
    mkacPlayer.latencyUtils.addRealTimeTask(destroyTransaction) {
      val self = mkacPlayer.compensatedEntities.self
      for (id in entityIds) {
        mkacPlayer.crystalTracker.onEntityRemoved(id)
        val entity = mkacPlayer.compensatedEntities.getEntity(id) ?: continue
        entity.isDead = true
        if (self.riding === entity) {
          self.eject()
        }
      }
    }
    mkacPlayer.latencyUtils.addRealTimeTask(
      destroyTransaction + 1,
      Runnable {
        for (id in entityIds) {
          mkacPlayer.compensatedEntities.removeEntity(id)
          mkacPlayer.compensatedFireworks.removeFirework(id)
        }
      },
    )
  }

  private fun handleJoinGame(
    event: PacketSendEvent,
    join: WrapperPlayServerJoinGame,
    mkacPlayer: MKACPlayer,
  ) {
    mkacPlayer.tracking.onSequenceBreak()
    val gameMode = join.gameMode
    mkacPlayer.entityId = join.entityId
    mkacPlayer.gameMode = gameMode
    mkacPlayer.tracking.gameMode = gameMode.ordinal
    mkacPlayer.compensatedEntities.clear()
    mkacPlayer.compensatedWorld.clear()
    if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_17)) {
      runCatching { join.dimensionType.getMinY(mkacPlayer.user.clientVersion) }
        .onSuccess { mkacPlayer.compensatedWorld.updateMinHeight(it) }
    }
    event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
  }

  private fun handleRespawn(
    event: PacketSendEvent,
    respawn: WrapperPlayServerRespawn,
    mkacPlayer: MKACPlayer,
  ) {
    mkacPlayer.tracking.onSequenceBreak()
    mkacPlayer.gameMode = respawn.gameMode
    mkacPlayer.tracking.gameMode = respawn.gameMode.ordinal
    val dimensionMinY = runCatching {
      respawn.dimensionType.getMinY(mkacPlayer.user.clientVersion)
    }
    mkacPlayer.sendTransaction()
    mkacPlayer.latencyUtils.addRealTimeTask(
      mkacPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        mkacPlayer.compensatedEntities.clear()
        mkacPlayer.compensatedWorld.clear()
        dimensionMinY.onSuccess { mkacPlayer.compensatedWorld.updateMinHeight(it) }
      },
    )
    event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
  }

  private fun handlePositionAndLook(
    event: PacketSendEvent,
    wrapper: WrapperPlayServerPlayerPositionAndLook,
    mkacPlayer: MKACPlayer,
  ) {
    mkacPlayer.sendTransaction()
    event.tasksAfterSend.add(Runnable { mkacPlayer.sendTransaction() })
    val transactionId = mkacPlayer.transactions.lastTransactionSent.get()
    val location = Vector3d(wrapper.x, wrapper.y, wrapper.z)
    val flags = wrapper.relativeFlags
    mkacPlayer.pendingTeleports.add(
      MKACPlayer.TeleportData(location, wrapper.yaw, wrapper.pitch, flags, transactionId)
    )
  }

  private fun handlePlayerRotation(
    wrapper: WrapperPlayServerPlayerRotation,
    mkacPlayer: MKACPlayer,
  ) {
    mkacPlayer.sendTransaction()
    val transactionId = mkacPlayer.transactions.lastTransactionSent.get()
    val storedPitch =
      if (wrapper.isRelativePitch) {
        wrapper.pitch
      } else {
        (wrapper.pitch % FULL_CIRCLE_DEGREES).coerceIn(-MAX_PITCH, MAX_PITCH)
      }
    mkacPlayer.pendingRotations.add(
      MKACPlayer.RotationData(
        wrapper.yaw,
        storedPitch,
        wrapper.isRelativeYaw,
        wrapper.isRelativePitch,
        transactionId,
      )
    )
  }

  private fun addTransactionResponse(player: MKACPlayer, id: Short): Boolean {
    var data: TransactionStamp? = null
    var hasId = false

    for (entry in player.transactions.transactionsSent) {
      if (entry.id == id) {
        hasId = true
        break
      }
    }

    if (hasId) {
      do {
        data = player.transactions.transactionsSent.poll()
        if (data == null) break
        player.transactions.lastTransactionReceived.incrementAndGet()
      } while (data.id != id)

      player.latencyUtils.handleNettySyncTransaction(
        player.transactions.lastTransactionReceived.get()
      )
      player.transactions.lastTransReceivedTime.set(System.currentTimeMillis())

      if (data != null) {
        val rttMs =
          ((System.nanoTime() - data.timeNanos) / NANOS_PER_MILLI).toInt().coerceAtLeast(0)
        val prev = player.tracking.playerPing
        player.tracking.playerPing =
          if (prev == 0) rttMs else (prev * PING_EWMA_OLD_WEIGHT + rttMs) / PING_EWMA_TOTAL_WEIGHT
      }
    }
    return data != null
  }

  private fun isMojangStupid(
    player: MKACPlayer,
    flying: WrapperPlayClientPlayerFlying,
    event: PacketReceiveEvent,
  ) {
    if (
      player.packetStateData.lastPacketWasTeleport ||
        player.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21)
    ) {
      return
    }

    val location: Location = flying.location
    if (shouldProcessDuplicate(player, flying, location)) {
      handleDuplicatePacketAction(player, flying, location, event)
      player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true
      applyDuplicateRotation(player, location)
    }
  }

  private fun shouldProcessDuplicate(
    player: MKACPlayer,
    flying: WrapperPlayClientPlayerFlying,
    location: Location,
  ): Boolean {
    val threshold = player.getMovementThreshold()
    val inVehicle = player.compensatedEntities.self.riding != null
    val hasMovementAndRotation = flying.hasPositionChanged() && flying.hasRotationChanged()
    val sameGroundAndCloseClaim =
      flying.isOnGround == player.packetStateData.packetPlayerOnGround &&
        player.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_17) &&
        player.packetStateData.duplicatePacketFilterPosition.distanceSquared(location.position) <
          threshold * threshold
    return hasMovementAndRotation && (sameGroundAndCloseClaim || inVehicle)
  }

  private fun handleDuplicatePacketAction(
    player: MKACPlayer,
    flying: WrapperPlayClientPlayerFlying,
    location: Location,
    event: PacketReceiveEvent,
  ) {
    val serverVersion = PacketEvents.getAPI().serverManager.version
    if (player.isForceCancelDuplicatePacket()) {
      event.isCancelled = true
      return
    }

    if (serverVersion.isOlderThanOrEquals(ServerVersion.V_1_9)) {
      if (player.isCancelDuplicatePacket()) {
        event.isCancelled = true
      }
      return
    }

    flying.location =
      Location(player.packetStateData.duplicatePacketFilterPosition, location.yaw, location.pitch)
    event.markForReEncode(true)
  }

  private fun applyDuplicateRotation(player: MKACPlayer, location: Location) {
    if (player.isIgnoreDuplicatePacketRotation()) return

    val movement = player.movement
    if (movement.yaw != location.yaw || movement.pitch != location.pitch) {
      movement.lastYaw = movement.yaw
      movement.lastPitch = movement.pitch
    }
    movement.yaw = location.yaw
    movement.pitch = location.pitch
  }

  private companion object {
    const val DEATH_ANIMATION_STATUS = 3
    // 1.7 rounding
    const val TELEPORT_EPSILON = 1.0E-7
    const val LIVING_FLAGS_INDEX_MODERN = 8
    const val LIVING_FLAGS_INDEX_POSE = 7
    const val LIVING_FLAGS_INDEX_LEGACY = 6
    const val LIVING_FLAGS_INDEX_ANCIENT = 5
    const val GLIDING_FLAG = 0x80
    const val SWIMMING_FLAG = 0x10
    const val HOTBAR_START_SLOT = 36
    const val OFFHAND_SLOT = 45
    const val DEFAULT_ATTACK_SPEED = 4.0f
    const val RIPTIDE_FLAG = 0x04
    const val USE_ITEM_FLAG = 0x01
    const val OFFHAND_FLAG = 0x02
    const val FIREWORK_ATTACHED_INDEX_MODERN = 9
    val AGEABLE_BABY_INDEX_RANGE = 15..17
    const val SIZE_METADATA_MIN_INDEX = 15
    const val GROUND_SEARCH_OFFSET = 0.5000001
    const val BODY_OFFSET = 0.5
    const val HEAD_OFFSET = 1.6
    const val NANOS_PER_MILLI = 1_000_000L
    const val PING_EWMA_OLD_WEIGHT = 4
    const val PING_EWMA_TOTAL_WEIGHT = 5
  }
}
