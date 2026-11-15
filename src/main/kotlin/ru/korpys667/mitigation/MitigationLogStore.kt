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

import java.util.Locale
import ru.korpys667.config.ConfigManager
import ru.korpys667.database.DatabaseManager
import ru.korpys667.database.MitigationLogEntry
import ru.korpys667.player.MKACPlayer

class MitigationLogStore(
  private val databaseManager: DatabaseManager,
  private val configManager: ConfigManager,
  private val settings: () -> MitigationSettings,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  fun enabled(): Boolean = settings().logEnabled

  fun saveOnQuit(mkacPlayer: MKACPlayer) {
    val state = mkacPlayer.mitigation
    val rule = state.applied ?: return
    if (state.appliedAtMillis == 0L) return
    record(
      mkacPlayer,
      rule,
      state.appliedTier,
      state.appliedAtMillis,
      maxOf(state.peakScore, state.score),
    )
  }

  fun record(
    mkacPlayer: MKACPlayer,
    rule: MitigationRule,
    tier: MitigationTier,
    startedAt: Long,
    peak: Double,
  ) {
    if (!enabled()) return
    databaseManager.database.recordMitigation(
      mkacPlayer.uuid,
      MitigationLogEntry(
        serverName = configManager.config.getString("history.server-name", "server"),
        playerName = mkacPlayer.player.name,
        rule = rule.id,
        tier = tier.name.lowercase(Locale.US),
        score = peak,
        startedAt = startedAt,
        endedAt = clock(),
      ),
    )
  }
}
