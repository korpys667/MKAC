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

import ru.korpys667.database.AiSnapshot
import ru.korpys667.database.DatabaseManager
import ru.korpys667.player.MKACPlayer

class AiSnapshotStore(
  private val databaseManager: DatabaseManager,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  fun saveOnQuit(mkacPlayer: MKACPlayer) {
    val state = mkacPlayer.mitigation
    if (state.answers == 0L) return
    val shape = state.shape()
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java)
    databaseManager.database.saveAiSnapshot(
      mkacPlayer.uuid,
      AiSnapshot(
        highWindows = aiCheck?.prob90?.toLong() ?: 0L,
        windows = state.answers,
        low = shape.low,
        middle = shape.middle,
        high = shape.high,
        savedAt = clock(),
      ),
    )
  }
}
