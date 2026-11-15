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
package ru.korpys667.monitor.core

import java.time.Duration
import java.time.Instant
import java.util.Locale
import org.bukkit.entity.Player
import ru.korpys667.checks.impl.ai.AiCheck
import ru.korpys667.data.CollectManager
import ru.korpys667.player.PlayerDataManager

class MonitorSampler(
  private val playerDataManager: PlayerDataManager,
  private val collectManager: CollectManager,
) {
  fun sample(target: Player): MonitorSample {
    val mkacTarget = playerDataManager.getPlayer(target)
    val aiCheck = mkacTarget?.checkManager?.getCheck(AiCheck::class.java)
    return MonitorSample(
      targetId = target.uniqueId,
      targetName = target.name,
      dataPresent = mkacTarget != null,
      aiActive = aiCheck != null,
      probability =
        MonitorLabelInfo.probabilityOfLeader(
          aiCheck?.labelBufferSnapshot().orEmpty(),
          aiCheck?.lastLabelProbabilities.orEmpty(),
          aiCheck?.lastCheatProbability ?: 0.0,
        ),
      buffer = aiCheck?.buffer ?: 0.0,
      rawPing = target.ping,
      damageMultiplier = mkacTarget?.combat?.damageMultiplier ?: 1.0,
      prob90 = aiCheck?.prob90 ?: 0,
      collect = collectInfo(target),
      inference = aiCheck?.let { MonitorInferenceInfo(inferenceStatus(it.inferenceProgress)) },
      leadingLabel = aiCheck?.let { leadingLabel(it.labelBufferSnapshot()) },
      labelBuffers = aiCheck?.labelBufferSnapshot().orEmpty(),
      labelProbabilities = aiCheck?.lastLabelProbabilities.orEmpty(),
      declaredLabels = aiCheck?.declaredLabels.orEmpty(),
      tier = mkacTarget?.mitigation?.appliedTier?.name ?: "NONE",
      score = mkacTarget?.mitigation?.score ?: 0.0,
      rule = mkacTarget?.mitigation?.applied?.id.orEmpty(),
      appliedForMillis = appliedFor(mkacTarget),
    )
  }

  private fun leadingLabel(buffers: Map<String, Double>): MonitorLabelInfo? =
    MonitorLabelInfo.leading(buffers)

  private fun collectInfo(target: Player): MonitorCollectInfo? {
    val session = collectManager.getSession(target.uniqueId) ?: return null
    val elapsed = Duration.between(session.startTime, Instant.now())
    return MonitorCollectInfo(
      status =
        tickStatus(
          collectManager.getCurrentProgress(target.uniqueId),
          collectManager.getPostWindow(),
        ),
      label = session.label.lowercase(Locale.ROOT),
      windows = session.windowCount(),
      elapsed = "${elapsed.toMinutes()}m${elapsed.toSecondsPart()}s",
    )
  }

  private fun tickStatus(progress: Int, postWindow: Int): String =
    if (progress < 0) WAITING else "$progress/$postWindow"

  private fun inferenceStatus(progress: IntArray?): String =
    if (progress == null) WAITING else "${progress[0]}/${progress[1]}"

  private fun appliedFor(mkacTarget: ru.korpys667.player.MKACPlayer?): Long {
    val state = mkacTarget?.mitigation
    val since = state?.appliedAtMillis ?: 0L
    return if (state?.applied == null || since == 0L) {
      0L
    } else {
      (System.currentTimeMillis() - since).coerceAtLeast(0L)
    }
  }

  private companion object {
    const val WAITING = "waiting"
  }
}
