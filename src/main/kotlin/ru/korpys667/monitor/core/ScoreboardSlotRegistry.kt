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

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

enum class SlotLossReason {
  DISPLACED,
  OBJECTIVE_REMOVED,
}

fun interface SlotLostCallback {
  fun onSlotLost(viewer: Player, reason: SlotLossReason, foreignObjective: String)
}

data class SlotClaim(val slot: Int, val objective: String, val onLost: SlotLostCallback)

class ScoreboardSlotRegistry {
  private val claims = ConcurrentHashMap<UUID, List<SlotClaim>>()

  @Volatile private var idle = true

  fun claim(viewerId: UUID, claim: SlotClaim) {
    claims.compute(viewerId) { _, current ->
      current.orEmpty().filterNot { it.slot == claim.slot } + claim
    }
    idle = false
  }

  fun release(viewerId: UUID, slot: Int) {
    claims.compute(viewerId) { _, current ->
      current?.filterNot { it.slot == slot }?.takeIf { it.isNotEmpty() }
    }
    idle = claims.isEmpty()
  }

  fun claimsFor(viewerId: UUID): List<SlotClaim>? = claims[viewerId]

  fun isIdle(): Boolean = idle
}
