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

import ru.korpys667.database.DatabaseManager
import ru.korpys667.database.StoredScore
import ru.korpys667.player.MKACPlayer
import ru.korpys667.scheduler.SchedulerService

class MitigationScoreStore(
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val settings: () -> MitigationSettings,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  fun restoreOnLogin(mkacPlayer: MKACPlayer) {
    val score = settings().score
    if (!score.persistEnabled) return

    scheduler.runAsync {
      val stored = databaseManager.database.loadMitigationScore(mkacPlayer.uuid) ?: return@runAsync
      if (!mkacPlayer.player.isOnline) return@runAsync

      val now = clock()
      val age = now - stored.updatedAt
      if (age < 0L || age > score.persistTtlMillis) return@runAsync

      val decayed = ScoreMath.decay(stored.score, age, score.halfLifeMillis)
      scheduler.runSync(mkacPlayer.player) {
        if (!mkacPlayer.player.isOnline) return@runSync
        mkacPlayer.mitigation.restore(decayed, now, score)
        mkacPlayer.mitigation.history = ScoreHistory(stored.sessions, stored.days, stored.lastDay)
      }
    }
  }

  fun save(mkacPlayer: MKACPlayer) {
    if (!settings().score.persistEnabled) return

    val state = mkacPlayer.mitigation
    val seen = state.history
    if (state.score == 0.0 && seen == ScoreHistory.EMPTY) return

    databaseManager.database.saveMitigationScore(
      mkacPlayer.uuid,
      StoredScore(
        score = state.score,
        updatedAt = clock(),
        sessions = seen.sessions,
        days = seen.days,
        lastDay = seen.lastDay,
      ),
    )
  }
}
