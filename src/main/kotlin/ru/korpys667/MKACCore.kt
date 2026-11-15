/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
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
package ru.korpys667

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.netty.channel.ChannelHelper
import java.util.logging.Level
import java.util.logging.Logger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.ServicePriority
import ru.korpys667.ai.label.VerdictResolver
import ru.korpys667.alert.AlertManager
import ru.korpys667.api.MKACApi
import ru.korpys667.command.CommandManager
import ru.korpys667.config.ConfigManager
import ru.korpys667.config.LocaleManager
import ru.korpys667.coroutines.MKACCoroutines
import ru.korpys667.data.CollectManager
import ru.korpys667.data.CollectSession
import ru.korpys667.database.DatabaseManager
import ru.korpys667.debug.DebugManager
import ru.korpys667.mitigation.MitigationRuntime
import ru.korpys667.monitor.MonitorServices
import ru.korpys667.packet.PacketListener
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.redis.CrossServerServices
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.server.AIServerProvider
import ru.korpys667.telemetry.TelemetryService
import ru.korpys667.utils.MessageUtil

class MKACCore
@Suppress("LongParameterList")
constructor(
  private val plugin: MKAC,
  private val playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val aiServerProvider: AIServerProvider,
  private val commandManager: CommandManager,
  private val alertManager: AlertManager,
  private val databaseManager: DatabaseManager,
  private val crossServer: CrossServerServices,
  private val debugManager: DebugManager,
  private val packetListener: PacketListener,
  private val monitor: MonitorServices,
  private val mitigationRuntime: MitigationRuntime,
  private val mkacApi: MKACApi,
  private val adventure: BukkitAudiences,
  private val coroutines: MKACCoroutines,
  private val scheduler: SchedulerService,
  private val telemetryService: TelemetryService,
  private val collectManager: CollectManager,
  private val logger: Logger,
) {
  fun enable() {
    CollectSession.sweepStaging(plugin.dataFolder)
    commandManager.registerCommands()

    MessageUtil.init(localeManager, adventure, plugin.logger)

    initializePacketRuntime()
    mitigationRuntime.enable()
    monitor.runtime.enable()
    plugin.server.servicesManager.register(
      MKACApi::class.java,
      mkacApi,
      plugin,
      ServicePriority.Normal,
    )
    scheduler.runAsync { crossServer.start() }
    telemetryService.start()
  }

  fun disable() {
    runCatching { PacketEvents.getAPI().eventManager.unregisterListener(packetListener) }
    // Saves must finish before the scheduler stops running queued work.
    runCatching { collectManager.saveAll() }
    runCatching { playerDataManager.saveAllBuffersSync() }
    runCatching { scheduler.cancelTasks() }
    runCatching { telemetryService.stop() }
    plugin.server.servicesManager.unregister(MKACApi::class.java, mkacApi)
    runCatching { mitigationRuntime.disable() }
    runCatching { aiServerProvider.shutdownTransport() }
    runCatching { crossServer.shutdown() }
    adventure.close()
    coroutines.close()
    databaseManager.shutdown()
    monitor.runtime.disable()
    runCatching { telemetryService.sendFarewell() }
  }

  fun reload() {
    configManager.reloadConfig()
    VerdictResolver.forgetReports()
    localeManager.reload()
    debugManager.reload()
    alertManager.reload()
    aiServerProvider.reload()
    playerDataManager.reloadAllPlayers()
    mitigationRuntime.reload()
    monitor.runtime.reload()
    monitor.view.reload()
    crossServer.stopMirrors()
    scheduler.runAsync {
      crossServer.redis.shutdown()
      crossServer.start()
    }
  }

  private fun initializePacketRuntime() {
    PacketEvents.getAPI().eventManager.registerListener(packetListener)
    PacketEvents.getAPI().eventManager.registerListener(monitor.slotObserver)
    monitor.view.start()
    PacketEvents.getAPI().init()
    trackAlreadyOnlinePlayers()
    scheduler.runTimer({ pollAllPlayers() }, 0L, 1L)
  }

  private fun trackAlreadyOnlinePlayers() {
    for (player in plugin.server.onlinePlayers) {
      val user = runCatching { PacketEvents.getAPI().playerManager.getUser(player) }.getOrNull()
      if (user == null) {
        logger.warning("No PacketEvents user for ${player.name}; leaving them untracked")
        continue
      }
      playerDataManager.handleUserLogin(user, player)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun pollAllPlayers() {
    for (mkacPlayer in playerDataManager.getSessions()) {
      try {
        if (!ChannelHelper.isOpen(mkacPlayer.user.channel)) {
          playerDataManager.handleUserDisconnect(mkacPlayer.user)
        } else if (mkacPlayer.isAttached) {
          mkacPlayer.pollData()
        }
      } catch (e: Exception) {
        logger.log(Level.WARNING, "Polling ${mkacPlayer.name} failed", e)
      }
    }
  }
}
