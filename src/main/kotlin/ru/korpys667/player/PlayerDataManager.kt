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
package ru.korpys667.player

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.User
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import ru.korpys667.MKAC
import ru.korpys667.alert.AlertManager
import ru.korpys667.alert.AlertType
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.checks.CheckManager
import ru.korpys667.checks.impl.ai.AiSnapshotStore
import ru.korpys667.checks.impl.ai.PersistentBufferService
import ru.korpys667.config.ConfigManager
import ru.korpys667.data.CollectManager
import ru.korpys667.database.DatabaseManager
import ru.korpys667.integration.GeyserUtil
import ru.korpys667.mitigation.MitigationLogStore
import ru.korpys667.mitigation.MitigationScoreStore
import ru.korpys667.punishment.PunishmentManager
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.server.AIServerProvider

@Suppress("TooManyFunctions")
class PlayerDataManager
@Suppress("LongParameterList")
constructor(
  private val plugin: MKAC,
  private val alertManager: AlertManager,
  private val collectManager: CollectManager,
  private val configManager: ConfigManager,
  private val aiServerProvider: AIServerProvider,
  private val exemptManager: ExemptManager,
  private val scheduler: SchedulerService,
  private val checkManagerFactory: CheckManager.Factory,
  private val punishmentManagerFactory: PunishmentManager.Factory,
  private val eventBus: MKACEventBus,
  private val databaseManager: DatabaseManager,
  private val persistentBufferService: PersistentBufferService,
  private val mitigationScoreStore: MitigationScoreStore,
  private val mitigationLogStore: MitigationLogStore,
  private val aiSnapshotStore: AiSnapshotStore,
) {
  private val players = ConcurrentHashMap<User, MKACPlayer>()
  private val unsavedBuffers = ConcurrentHashMap.newKeySet<MKACPlayer>()

  @Suppress("ReturnCount")
  private fun cleanupPlayer(tracked: MKACPlayer) {
    if (!players.remove(tracked.user, tracked)) {
      return
    }
    val uuid = tracked.uuid
    if (players.values.any { it.uuid == uuid }) {
      return
    }
    val player = tracked.playerOrNull
    if (collectManager.getSession(uuid) != null) {
      collectManager.stopCollecting(uuid)
    }
    if (player != null) {
      runCatching { alertManager.handlePlayerQuit(player) }
        .onFailure {
          plugin.logger.log(
            java.util.logging.Level.WARNING,
            "alertManager.handlePlayerQuit failed for ${player.name}",
            it,
          )
        }
    }
    if (!tracked.isAttached) return
    // runAsync only starts on the next heartbeat, so the write is not guaranteed by shutdown.
    unsavedBuffers.add(tracked)
    scheduler.runAsync {
      if (unsavedBuffers.remove(tracked)) {
        persistentBufferService.saveOnQuit(tracked)
        mitigationScoreStore.save(tracked)
        mitigationLogStore.saveOnQuit(tracked)
        aiSnapshotStore.saveOnQuit(tracked)
      }
    }
  }

  fun saveAllBuffersSync() {
    for (mkacPlayer in players.values) {
      if (mkacPlayer.isAttached) {
        persistentBufferService.saveOnShutdown(mkacPlayer)
      }
    }
    val iterator = unsavedBuffers.iterator()
    while (iterator.hasNext()) {
      val mkacPlayer = iterator.next()
      iterator.remove()
      persistentBufferService.saveOnShutdown(mkacPlayer)
      mitigationScoreStore.save(mkacPlayer)
      aiSnapshotStore.saveOnQuit(mkacPlayer)
    }
  }

  fun getPlayer(player: Player?): MKACPlayer? {
    if (player == null) {
      return null
    }
    return getPlayer(player.uniqueId)
  }

  fun getPlayer(uuid: UUID): MKACPlayer? {
    val byChannel = resolveUser(uuid)?.let { players[it] }
    val tracked = byChannel ?: players.values.firstOrNull { it.uuid == uuid }
    return tracked?.takeIf { it.isAttached }
  }

  fun getPlayer(user: User?): MKACPlayer? {
    if (user == null) {
      return null
    }
    return players[user]
  }

  private fun resolveUser(uuid: UUID): User? =
    runCatching {
        val protocol = PacketEvents.getAPI().protocolManager
        val channel = protocol.getChannel(uuid) ?: return@runCatching null
        protocol.getUser(channel)
      }
      .getOrNull()

  fun handleUserConnect(user: User) {
    if (user.uuid == null || players.containsKey(user)) {
      return
    }
    players[user] =
      MKACPlayer(
        user = user,
        plugin = plugin,
        configManager = configManager,
        alertManager = alertManager,
        aiServerProvider = aiServerProvider,
        exemptManager = exemptManager,
        scheduler = scheduler,
        checkManagerFactory = checkManagerFactory,
        punishmentManagerFactory = punishmentManagerFactory,
        eventBus = eventBus,
      )
  }

  fun getPlayers(): Collection<MKACPlayer> {
    return players.values.filter { it.isAttached }
  }

  fun getSessions(): Collection<MKACPlayer> {
    return players.values.toList()
  }

  fun handleUserLogin(user: User, player: Player) {
    scheduler.runSync(
      player,
      Runnable {
        if (!player.isOnline) {
          return@Runnable
        }
        handleUserConnect(user)
        val mkacPlayer = players[user] ?: return@Runnable
        if (mkacPlayer.isAttached) {
          return@Runnable
        }
        val playerUuid = player.uniqueId
        mkacPlayer.entityId = user.entityId.takeIf { it > 0 } ?: player.entityId
        applyInitialWorldState(mkacPlayer, player)
        mkacPlayer.isBedrock = GeyserUtil.isBedrockPlayer(playerUuid)

        if (players[user] !== mkacPlayer) {
          return@Runnable
        }
        mkacPlayer.attach(player)

        val loginTimestamp = System.currentTimeMillis()
        scheduler.runAsync { databaseManager.database.recordLogin(playerUuid, loginTimestamp) }
        persistentBufferService.restoreOnLogin(mkacPlayer)
        mitigationScoreStore.restoreOnLogin(mkacPlayer)

        enableAlertsOnJoin(player)
      },
    )
  }

  private fun enableAlertsOnJoin(player: Player) {
    AlertType.entries.forEach { type ->
      if (
        player.hasPermission(type.permission) &&
          player.hasPermission("${type.permission}.enable-on-join") &&
          !alertManager.hasAlertsEnabled(player, type)
      ) {
        alertManager.toggle(player, type, true)
      }
    }
  }

  private fun applyInitialWorldState(mkacPlayer: MKACPlayer, player: Player) {
    val gameMode =
      com.github.retrooper.packetevents.protocol.player.GameMode.getById(player.gameMode.value)
    if (gameMode != null) {
      mkacPlayer.gameMode = gameMode
      mkacPlayer.tracking.gameMode = gameMode.ordinal
    }
    mkacPlayer.compensatedWorld.updateMinHeight(player.world.minHeight)
  }

  fun handleUserDisconnect(user: User) {
    val tracked = players[user] ?: return
    cleanupPlayer(tracked)
  }

  fun reloadAllPlayers() {
    for (mkacPlayer in players.values) {
      mkacPlayer.reload()
    }
  }
}
