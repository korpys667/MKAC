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
package ru.korpys667.monitor.hud

import org.bukkit.entity.Player
import ru.korpys667.monitor.core.MonitorOutputKind

class MonitorOutputRegistry(outputs: List<MonitorOutput>) {
  private val byKind: Map<MonitorOutputKind, MonitorOutput> = outputs.associateBy { it.kind }

  fun output(kind: MonitorOutputKind): MonitorOutput? = byKind[kind]

  fun capacity(kind: MonitorOutputKind): Int = byKind[kind]?.capabilities?.maxTargets ?: 1

  fun isSupported(kind: MonitorOutputKind): Boolean = byKind[kind]?.isAvailable() ?: false

  fun available(config: MonitorHudRuntimeConfig): List<MonitorOutputKind> =
    MonitorOutputKind.entries.filter { kind ->
      val output = byKind[kind]
      output != null && output.isAvailable() && config.isEnabled(kind)
    }

  fun usable(kind: MonitorOutputKind, viewer: Player, config: MonitorHudRuntimeConfig): Boolean =
    byKind[kind]?.let { config.isEnabled(kind) && it.isAvailableFor(viewer) } ?: false

  fun resolveAll(
    preferred: Set<MonitorOutputKind>,
    fallback: Set<MonitorOutputKind>,
    viewer: Player,
    config: MonitorHudRuntimeConfig,
  ): List<MonitorOutput> {
    val chosen = preferred.filter { usable(it, viewer, config) }
    if (chosen.isNotEmpty()) {
      return chosen.mapNotNull { byKind[it] }
    }
    val backup = (fallback + MonitorOutputKind.ACTIONBAR).firstOrNull { usable(it, viewer, config) }
    return listOfNotNull(backup?.let { byKind[it] })
  }
}
