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
package ru.korpys667.data

import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager
import ru.korpys667.player.MKACPlayer
import ru.korpys667.scheduler.SchedulerService

class CollectManager(
  private val plugin: MKAC,
  private val configManager: ConfigManager,
  private val scheduler: SchedulerService,
) {
  private val datasetUploader = CollectDatasetUploader(plugin, configManager)
  val activeSessions: MutableMap<UUID, CollectSession> = ConcurrentHashMap()
  private val windows: MutableMap<UUID, AttackWindowTracker> = ConcurrentHashMap()
  private val unsavedSessions: MutableMap<UUID, CollectSession> = ConcurrentHashMap()

  fun startCollecting(mkacPlayer: MKACPlayer, label: String): Boolean {
    val uuid = mkacPlayer.uuid
    if (activeSessions.containsKey(uuid)) return false
    mkacPlayer.tracking.pendingSequenceBreak = true
    mkacPlayer.tracking.pendingBufferReset = true

    activeSessions[uuid] =
      CollectSession(
        sessionId = UUID.randomUUID(),
        playerUuid = uuid,
        playerName = mkacPlayer.player.name,
        label = label,
        serverVersion = plugin.server.version,
        clientVersion = mkacPlayer.user.clientVersion.protocolVersion,
      )
    windows[uuid] = AttackWindowTracker()
    return true
  }

  fun stopCollecting(uuid: UUID): Boolean {
    val session = removeSession(uuid) ?: return false
    // runAsync only starts on the next heartbeat, so the write is not guaranteed by shutdown.
    unsavedSessions[session.sessionId] = session
    scheduler.runAsync {
      if (unsavedSessions.remove(session.sessionId, session)) {
        writeSession(session)
      }
    }
    return true
  }

  private fun writeSession(session: CollectSession) {
    try {
      val savedDirectory = session.save(plugin.dataFolder)
      if (savedDirectory != null) datasetUploader.upload(savedDirectory)
    } catch (e: IOException) {
      plugin.logger.log(Level.SEVERE, "Failed to save data for ${session.playerName}", e)
    }
  }

  fun cancelCollecting(uuid: UUID): Boolean = removeSession(uuid) != null

  fun getSession(uuid: UUID): CollectSession? = activeSessions[uuid]

  fun saveAll() {
    val uuids = activeSessions.keys.toList()
    for (uuid in uuids) {
      val session = removeSession(uuid) ?: continue
      writeSession(session)
    }
    for ((id, session) in unsavedSessions) {
      if (unsavedSessions.remove(id, session)) {
        writeSession(session)
      }
    }
  }

  fun getCurrentProgress(uuid: UUID): Int = windows[uuid]?.ticksSinceAttack ?: -1

  fun getPostWindow(): Int = configManager.collectPostWindow

  fun onTick(mkacPlayer: MKACPlayer) {
    val uuid = mkacPlayer.uuid
    val session = activeSessions[uuid]
    if (session == null) {
      windows.remove(uuid)
      return
    }

    val tracker = windows.getOrPut(uuid) { AttackWindowTracker() }
    tracker.onTick(
      mkacPlayer.tickBuffer,
      mkacPlayer.tracking.windowStartThisTick,
      mkacPlayer.tracking.windowStartKind,
      configManager.collectPostWindow,
    ) { ticks, attackIndex, kind ->
      val window =
        mkacPlayer.tickBuffer.extractWindow(
          configManager.collectPreWindow,
          configManager.collectPostWindow,
          ticks,
          attackIndex,
          kind,
        )
      if (window != null) session.addWindow(window)
    }
  }

  private fun removeSession(uuid: UUID): CollectSession? {
    val session = activeSessions.remove(uuid) ?: return null
    windows.remove(uuid)
    return session
  }
}
