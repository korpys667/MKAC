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
package ru.korpys667.checks.impl.ai

import java.util.Locale
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min
import ru.korpys667.config.ConfigManager
import ru.korpys667.database.AiBufferState
import ru.korpys667.database.DatabaseManager
import ru.korpys667.debug.DebugCategory
import ru.korpys667.debug.DebugManager
import ru.korpys667.player.MKACPlayer
import ru.korpys667.scheduler.SchedulerService

private const val MILLIS_PER_HOUR = 3_600_000.0

class PersistentBufferService(
  private val configManager: ConfigManager,
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val debugManager: DebugManager,
  private val logger: Logger,
) {
  fun restoreOnLogin(mkacPlayer: MKACPlayer) {
    if (!configManager.persistentBufferEnabled) return
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java) ?: return

    scheduler.runAsync {
      val scalar = databaseManager.database.loadAiBuffer(mkacPlayer.uuid)
      val labelStates = databaseManager.database.loadAiLabelBuffers(mkacPlayer.uuid)
      if (scalar == null && labelStates.isEmpty()) return@runAsync
      if (!mkacPlayer.player.isOnline) return@runAsync

      val now = System.currentTimeMillis()
      val playerName = mkacPlayer.player.name
      val restored = LinkedHashMap<String, Double>()

      for ((label, state) in labelStates) {
        surviving(state, now, playerName)?.let { restored[label] = it }
      }

      if (labelStates.isEmpty() && scalar != null) {
        surviving(scalar, now, playerName)?.let { restored[AiCheck.UNATTRIBUTED_LABEL] = it }
      }

      if (restored.isEmpty()) {
        debugManager.log(
          DebugCategory.AI_PERSISTENT_BUFFER,
          "$playerName had nothing left to restore (expired or decayed to zero)",
        )
        return@runAsync
      }

      scheduler.runSync(mkacPlayer.player) {
        if (!mkacPlayer.player.isOnline) return@runSync
        for ((label, value) in restored) {
          aiCheck.restoreLabelBuffer(label, value)
        }
      }
      debugManager.log(
        DebugCategory.AI_PERSISTENT_BUFFER,
        "$playerName restored " +
          restored.entries.joinToString(", ") { "${it.key}=${format(it.value)}" },
      )
    }
  }

  private fun surviving(state: AiBufferState, now: Long, playerName: String): Double? {
    val ageMillis = now - state.updatedAt
    return when {
      ageMillis < 0L -> {
        logger.warning(
          "[PersistentBuffer] Skipped restore for $playerName: stored timestamp is in the future"
        )
        null
      }
      ageMillis < configManager.persistentBufferDisconnectWindowMillis -> state.buffer
      ageMillis > configManager.persistentBufferTtlMillis -> null
      else -> decayAndCap(state.buffer, ageMillis / MILLIS_PER_HOUR).takeIf { it > 0.0 }
    }
  }

  fun saveOnQuit(mkacPlayer: MKACPlayer) {
    if (!configManager.persistentBufferEnabled) return
    val now = System.currentTimeMillis()
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java) ?: return

    val threshold = configManager.persistentBufferSaveThreshold
    if (aiCheck.buffer >= threshold) {
      databaseManager.database.saveAiBuffer(mkacPlayer.uuid, aiCheck.buffer, now)
    } else {
      databaseManager.database.saveAiBuffer(mkacPlayer.uuid, 0.0, 0L)
    }

    val labelBuffers = aiCheck.labelBufferSnapshot().filterValues { it >= threshold }
    databaseManager.database.saveAiLabelBuffers(mkacPlayer.uuid, labelBuffers, now)
  }

  fun saveOnShutdown(mkacPlayer: MKACPlayer) {
    saveOnQuit(mkacPlayer)
  }

  private fun decayAndCap(saved: Double, ageHours: Double): Double {
    val decayed = saved - configManager.persistentBufferDecayPerHour * ageHours
    return max(0.0, min(decayed, configManager.persistentBufferCap))
  }

  private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}
