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
package ru.korpys667.mitigation

import ru.korpys667.config.ConfigManager
import ru.korpys667.monitor.hud.MILLIS_PER_TICK
import ru.korpys667.player.MKACPlayer

class MitigationScorer(
  private val configManager: ConfigManager,
  private val settings: () -> MitigationSettings,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  fun record(
    mkacPlayer: MKACPlayer,
    probability: Double,
    labels: Map<String, Double> = emptyMap(),
  ) {
    val config = settings()
    val now = clock()
    val contribution =
      ScoreMath.contribution(
        probability,
        configManager.aiStep,
        (configManager.aiPreWindow + configManager.aiPostWindow),
        config.score,
      )
    mkacPlayer.mitigation.record(contribution, probability, now, config.score)
    mkacPlayer.mitigation.noteProbability(
      probability,
      now,
      HoldAccounting(
        config.probabilityHolds,
        (configManager.aiPreWindow + configManager.aiPostWindow) * MILLIS_PER_TICK,
        config.score.forgetRate,
      ),
      labels,
    )
  }

  fun leak(mkacPlayer: MKACPlayer) {
    mkacPlayer.mitigation.leak(clock(), settings().score)
  }

  fun freeze(mkacPlayer: MKACPlayer) {
    mkacPlayer.mitigation.freeze(clock(), settings().score)
  }

  fun thaw(mkacPlayer: MKACPlayer) {
    mkacPlayer.mitigation.thaw(clock())
  }
}
