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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.korpys667.player

import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag
import com.github.retrooper.packetevents.util.Vector3d
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import ru.korpys667.MKAC
import ru.korpys667.alert.AlertManager
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.checks.CheckManager
import ru.korpys667.config.ConfigManager
import ru.korpys667.data.CrystalTracker
import ru.korpys667.data.TickBuffer
import ru.korpys667.entity.CompensatedEntities
import ru.korpys667.entity.CompensatedFireworks
import ru.korpys667.mitigation.MitigationState
import ru.korpys667.player.state.CombatState
import ru.korpys667.player.state.MovementState
import ru.korpys667.player.state.TrackingState
import ru.korpys667.player.state.TransactionTracker
import ru.korpys667.punishment.PunishmentManager
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.server.AIServerProvider
import ru.korpys667.utils.data.HeadRotation
import ru.korpys667.utils.data.PacketStateData
import ru.korpys667.utils.latency.ILatencyUtils
import ru.korpys667.utils.latency.LatencyUtils
import ru.korpys667.utils.update.RotationUpdate
import ru.korpys667.world.CompensatedWorld

@Suppress("TooManyFunctions")
class MKACPlayer
@Suppress("LongParameterList", "UnusedPrivateProperty")
constructor(
  val user: User,
  private val plugin: MKAC,
  private val configManager: ConfigManager,
  alertManager: AlertManager,
  aiServerProvider: AIServerProvider,
  val exemptManager: ExemptManager,
  private val scheduler: SchedulerService,
  checkManagerFactory: CheckManager.Factory,
  punishmentManagerFactory: PunishmentManager.Factory,
  val eventBus: MKACEventBus,
) {
  val uuid: UUID = user.uuid
  val name: String = user.profile?.name ?: uuid.toString()

  @Volatile private var attachedPlayer: Player? = null

  val playerOrNull: Player?
    get() = attachedPlayer

  val isAttached: Boolean
    get() = attachedPlayer != null

  val player: Player
    get() = attachedPlayer ?: error("Bukkit player for $name is not attached yet")

  fun attach(bukkitPlayer: Player) {
    val now = System.currentTimeMillis()
    transactions.lastTransSentTime.set(now)
    transactions.lastTransReceivedTime.set(now)
    attachedPlayer = bukkitPlayer
  }

  val packetStateData: PacketStateData = PacketStateData()
  val rotationUpdate: RotationUpdate = RotationUpdate(HeadRotation(), HeadRotation(), 0f, 0f)
  val joinTime: Long = System.currentTimeMillis()

  var entityId: Int = 0
  var gameMode: GameMode = GameMode.SURVIVAL
  var brand: String = "vanilla"
  var isBedrock: Boolean = false

  val isBedrockExempt: Boolean
    get() = configManager.isBedrockExemptEnabled() && isBedrock

  val movement: MovementState = MovementState()
  val combat: CombatState = CombatState()
  val tracking: TrackingState = TrackingState()
  val mitigation: MitigationState = MitigationState()
  val transactions: TransactionTracker = TransactionTracker()
  @Volatile
  var tickBuffer: TickBuffer = TickBuffer(requiredBufferCapacity())
    private set

  val pendingTeleports: Queue<TeleportData> = ConcurrentLinkedQueue()
  val pendingRotations: Queue<RotationData> = ConcurrentLinkedQueue()

  val crystalTracker: CrystalTracker = CrystalTracker()
  val compensatedEntities: CompensatedEntities = CompensatedEntities(this)
  val compensatedFireworks: CompensatedFireworks = CompensatedFireworks()
  val compensatedWorld: CompensatedWorld = CompensatedWorld(this)
  val latencyUtils: ILatencyUtils = LatencyUtils(this, plugin)
  val checkManager: CheckManager = checkManagerFactory.create(this)
  val punishmentManager: PunishmentManager = punishmentManagerFactory.create(this)

  private val hasDisconnected = java.util.concurrent.atomic.AtomicBoolean(false)

  private var cancelDuplicatePacket = true
  private var forceCancelDuplicatePacket = false
  private var ignoreDuplicatePacketRotation = true

  init {
    refreshDuplicatePacketSettings()
  }

  fun isPointThree(): Boolean = user.clientVersion.isOlderThan(ClientVersion.V_1_18_2)

  @Suppress("MagicNumber") fun getMovementThreshold(): Double = if (isPointThree()) 0.03 else 0.0002

  fun isCancelDuplicatePacket(): Boolean = cancelDuplicatePacket

  fun isForceCancelDuplicatePacket(): Boolean = forceCancelDuplicatePacket

  fun isIgnoreDuplicatePacketRotation(): Boolean = ignoreDuplicatePacketRotation

  fun sendTransaction() {
    transactions.sendTransaction(user)
  }

  fun pollData() {
    val now = System.currentTimeMillis()
    if (user.encoderState != com.github.retrooper.packetevents.protocol.ConnectionState.PLAY) {
      transactions.lastTransReceivedTime.set(now)
      return
    }
    if (now - transactions.lastTransSentTime.get() > TRANSACTION_KEEPALIVE_MS) {
      sendTransaction()
    }
    if (now - transactions.lastTransReceivedTime.get() > TRANSACTION_TIMEOUT_MS) {
      disconnect(Component.text("Transaction timeout"))
    }
  }

  @Suppress("TooGenericExceptionCaught")
  fun disconnect(reason: Component) {
    if (!hasDisconnected.compareAndSet(false, true)) {
      return
    }
    try {
      user.sendPacket(
        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect(reason)
      )
    } catch (e: Exception) {
      plugin.logger.warning("[MKAC] Disconnect packet for $name failed: ${e.message}")
    }
    user.closeConnection()

    val bukkitPlayer = attachedPlayer ?: return
    scheduler.runSync(bukkitPlayer) { bukkitPlayer.kick(reason) }
  }

  fun reload() {
    refreshDuplicatePacketSettings()
    ensureTickBufferCapacity()
    punishmentManager.reload()
    checkManager.reloadChecks()
  }

  fun ensureTickBufferCapacity() {
    val required = requiredBufferCapacity()
    if (required > tickBuffer.capacity()) {
      tickBuffer = TickBuffer(required)
    }
  }

  private fun requiredBufferCapacity(): Int =
    maxOf(configManager.aiPreWindow, configManager.collectPreWindow) +
      maxOf(configManager.aiPostWindow, configManager.collectPostWindow) +
      1

  private fun refreshDuplicatePacketSettings() {
    tracking.enabledWindowStarts = configManager.enabledWindowStarts
    cancelDuplicatePacket = configManager.cancelDuplicatePacket
    forceCancelDuplicatePacket = configManager.forceCancelDuplicatePacket
    ignoreDuplicatePacketRotation = configManager.ignoreDuplicatePacketRotation
  }

  class TeleportData(
    val location: Vector3d,
    val yaw: Float,
    val pitch: Float,
    val flags: RelativeFlag,
    val transactionId: Int,
  ) {
    fun isRelativeX(): Boolean = flags.has(RelativeFlag.X)

    fun isRelativeY(): Boolean = flags.has(RelativeFlag.Y)

    fun isRelativeZ(): Boolean = flags.has(RelativeFlag.Z)

    fun isRelativePos(): Boolean = isRelativeX() || isRelativeY() || isRelativeZ()

    fun rotationMatches(actualYaw: Float, actualPitch: Float): Boolean =
      (flags.has(RelativeFlag.YAW) || angleMatches(actualYaw, yaw, yaw % FULL_TURN)) &&
        (flags.has(RelativeFlag.PITCH) ||
          angleMatches(actualPitch, pitch, pitch.coerceIn(MIN_PITCH, MAX_PITCH)))

    // Some versions wrap yaw and clamp pitch client-side, others apply the raw value, and the
    // server
    // sends it raw either way, so both forms have to be accepted.
    private fun angleMatches(actual: Float, raw: Float, adjusted: Float): Boolean =
      !raw.isFinite() || actual == raw || actual == adjusted

    private companion object {
      const val FULL_TURN = 360f
      const val MIN_PITCH = -90f
      const val MAX_PITCH = 90f
    }
  }

  class RotationData(
    val yaw: Float,
    val pitch: Float,
    val relativeYaw: Boolean,
    val relativePitch: Boolean,
    val transactionId: Int,
  )

  private companion object {
    const val TRANSACTION_KEEPALIVE_MS = 80L
    const val TRANSACTION_TIMEOUT_MS = 60_000L
  }
}
