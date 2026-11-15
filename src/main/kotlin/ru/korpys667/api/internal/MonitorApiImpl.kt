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
package ru.korpys667.api.internal

import java.util.Optional
import java.util.UUID
import ru.korpys667.api.model.MonitorSnapshot
import ru.korpys667.api.service.MonitorApi
import ru.korpys667.monitor.core.MonitorSampler
import ru.korpys667.player.PlayerDataManager

class MonitorApiImpl(
  private val playerDataManager: PlayerDataManager,
  private val sampler: MonitorSampler,
) : MonitorApi {
  override fun getSnapshot(playerId: UUID): Optional<MonitorSnapshot> {
    val mkacPlayer = playerDataManager.getPlayer(playerId) ?: return Optional.empty()
    val sample = sampler.sample(mkacPlayer.player)
    if (!sample.aiActive) {
      return Optional.empty()
    }
    return Optional.of(
      MonitorSnapshot(
        sample.probability,
        sample.buffer,
        sample.rawPing,
        sample.damageMultiplier,
        sample.prob90,
      )
    )
  }
}
