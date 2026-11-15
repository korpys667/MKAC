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
package ru.korpys667.monitor.view

import java.util.UUID
import org.bukkit.entity.Player
import ru.korpys667.MKAC
import ru.korpys667.monitor.core.ScoreboardPacketBridge
import ru.korpys667.monitor.core.SlotLossReason
import ru.korpys667.monitor.core.SlotLostCallback

internal class ViewSlotGuard(
  private val plugin: MKAC,
  private val tracker: ViewTargetTracker,
  private val bridge: ScoreboardPacketBridge,
  private val sessionProvider: (UUID) -> ViewSession?,
) : SlotLostCallback {
  override fun onSlotLost(viewer: Player, reason: SlotLossReason, foreignObjective: String) {
    val session = sessionProvider(viewer.uniqueId)?.takeIf(ViewSession::usesBelowName) ?: return
    val objectiveName = session.belowObjectiveName ?: return
    when (reason) {
      SlotLossReason.DISPLACED -> {
        logFirstConflict(session, viewer, foreignObjective)
        bridge.displayObjective(viewer, objectiveName, session.config.slot)
      }
      SlotLossReason.OBJECTIVE_REMOVED -> {
        session.targetTeams.values.forEach(TargetTeamState::invalidateBelowName)
        bridge.createObjective(viewer, session.config.objectiveSpec(objectiveName))
      }
    }
    tracker.refreshTrackedTargets(viewer, session)
  }

  private fun logFirstConflict(session: ViewSession, viewer: Player, conflictingObjective: String) {
    if (session.belowNameConflictLogged) {
      return
    }

    plugin.logger.warning(
      "[View] Viewer ${viewer.name} reasserted MKAC below-name display after " +
        "'$conflictingObjective' attempted to claim the slot."
    )
    session.belowNameConflictLogged = true
  }
}
