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
package ru.korpys667.monitor.hud.output

import org.bukkit.entity.Player
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.core.ObjectiveSpec
import ru.korpys667.monitor.core.ScoreDisplay
import ru.korpys667.monitor.core.ScoreboardPacketBridge
import ru.korpys667.monitor.core.ScoreboardSlotRegistry
import ru.korpys667.monitor.core.SlotClaim
import ru.korpys667.monitor.core.SlotLossReason
import ru.korpys667.monitor.hud.MonitorHudRuntimeConfig
import ru.korpys667.monitor.hud.MonitorOutput
import ru.korpys667.monitor.hud.MonitorOutputCapabilities
import ru.korpys667.monitor.hud.MonitorOutputPolicy
import ru.korpys667.monitor.hud.MonitorRenderContext
import ru.korpys667.monitor.hud.MonitorRenderPayload
import ru.korpys667.monitor.hud.OutputStates

class SidebarOutput(
  private val bridge: ScoreboardPacketBridge,
  private val slotRegistry: ScoreboardSlotRegistry,
) : MonitorOutput {
  private val states = OutputStates<SidebarState>()

  override val kind = MonitorOutputKind.SIDEBAR

  override val capabilities =
    MonitorOutputCapabilities(
      maxTargets = SIDEBAR_MAX_TARGETS,
      claimsClientSlot = true,
      eventDriven = false,
      requiresClear = true,
    )

  override fun isAvailable(): Boolean = bridge.serverSupportsFancyText()

  override fun isAvailableFor(viewer: Player): Boolean = bridge.supportsFancyText(viewer)

  override fun policy(config: MonitorHudRuntimeConfig): MonitorOutputPolicy =
    MonitorOutputPolicy(keepAliveCycles = 0, minIntervalCycles = 0)

  override fun attach(context: MonitorRenderContext): Boolean {
    val config = context.config.sidebar
    val state = SidebarState(sidebarObjectiveName(context.viewerId, context.sessionId), config.slot)
    states.put(context, state)?.let { displaced ->
      state.lastForeignObjective = displaced.lastForeignObjective
      if (context.viewer.isOnline && displaced.created) {
        displaced.lines.indices.forEach {
          bridge.removeEntry(context.viewer, displaced.objectiveName, sidebarEntryKey(it))
        }
        bridge.removeObjective(context.viewer, displaced.objectiveName)
      }
      displaced.created = false
      displaced.lines = emptyList()
    }
    createObjective(context, state)
    slotRegistry.claim(
      context.viewerId,
      SlotClaim(config.slot, state.objectiveName) { viewer, reason, foreign ->
        onSlotLost(context, viewer, reason, foreign)
      },
    )
    return true
  }

  override fun render(context: MonitorRenderContext, payload: MonitorRenderPayload) {
    val state = states.get(context)
    if (state == null || !context.viewer.isOnline) {
      return
    }
    if (!state.created) {
      createObjective(context, state)
    }
    val config = context.config.sidebar
    syncLines(context.viewer, state, buildSidebarLines(payload, config))

    state.cyclesSinceReassert++
    if (config.reassertCycles > 0 && state.cyclesSinceReassert >= config.reassertCycles) {
      bridge.displayObjective(context.viewer, state.objectiveName, state.slot)
      state.cyclesSinceReassert = 0
    }
  }

  override fun clear(context: MonitorRenderContext) {
    val state = states.get(context) ?: return
    val drawn = state.created
    val lines = state.lines
    state.created = false
    state.lines = emptyList()
    if (context.viewer.isOnline && drawn) {
      lines.indices.forEach {
        bridge.removeEntry(context.viewer, state.objectiveName, sidebarEntryKey(it))
      }
      bridge.removeObjective(context.viewer, state.objectiveName)
      restoreSidebar(bridge, context.viewer, state.slot, state.lastForeignObjective)
    }
  }

  override fun detach(context: MonitorRenderContext) {
    clear(context)
    val state = states.remove(context) ?: return
    slotRegistry.release(context.viewerId, state.slot)
  }

  private fun createObjective(context: MonitorRenderContext, state: SidebarState) {
    if (!context.viewer.isOnline) {
      return
    }
    val config = context.config.sidebar
    bridge.createObjective(
      context.viewer,
      ObjectiveSpec(
        name = state.objectiveName,
        slot = state.slot,
        title = config.title,
        legacyTitleFallback = config.title,
        blankScoreText = "",
      ),
    )
    state.created = true
    state.lines = emptyList()
    state.cyclesSinceReassert = 0
  }

  private fun syncLines(viewer: Player, state: SidebarState, next: List<String>) {
    val countChanged = next.size != state.lines.size
    next.forEachIndexed { index, line ->
      if (countChanged || state.lines[index] != line) {
        bridge.updateEntry(
          viewer,
          state.objectiveName,
          sidebarEntryKey(index),
          next.size - index,
          ScoreDisplay(entryDisplay = line, scoreText = null),
        )
      }
    }
    for (index in next.size until state.lines.size) {
      bridge.removeEntry(viewer, state.objectiveName, sidebarEntryKey(index))
    }
    state.lines = next
  }

  private fun onSlotLost(
    context: MonitorRenderContext,
    viewer: Player,
    reason: SlotLossReason,
    foreignObjective: String,
  ) {
    val state = states.get(context) ?: return
    if (foreignObjective.isNotBlank() && foreignObjective != state.objectiveName) {
      state.lastForeignObjective = foreignObjective
    }
    when (reason) {
      SlotLossReason.DISPLACED ->
        if (state.created) {
          bridge.displayObjective(viewer, state.objectiveName, state.slot)
          state.cyclesSinceReassert = 0
        }
      SlotLossReason.OBJECTIVE_REMOVED -> {
        state.created = false
        state.lines = emptyList()
      }
    }
  }

  internal class SidebarState(val objectiveName: String, val slot: Int) {
    var created = false
    var lines: List<String> = emptyList()
    var cyclesSinceReassert = 0
    var lastForeignObjective: String? = null
  }
}
