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

import java.util.UUID
import ru.korpys667.monitor.core.MonitorSample

enum class MonitorSeverity {
  CALM,
  WATCH,
  ALERT,
}

data class MonitorFrame(
  val targetId: UUID,
  val targetName: String,
  val headline: String,
  val placeholders: Map<String, String>,
  val progress: Float,
  val severity: MonitorSeverity,
  val dataPresent: Boolean,
  val aiActive: Boolean,
  val labels: List<MonitorFrameLabel> = emptyList(),
  val allLabels: Boolean = false,
)

data class MonitorFrameLabel(
  val key: String,
  val name: String,
  val buffer: String,
  val probability: String,
  val written: String = name,
)

class MonitorRenderPayload(val frames: List<MonitorFrame>, val samples: Map<UUID, MonitorSample>) {
  val primary: MonitorFrame
    get() = frames[0]
}
