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

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import ru.korpys667.monitor.core.ComponentCache
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.hud.MonitorFrame
import ru.korpys667.monitor.hud.MonitorHudRuntimeConfig
import ru.korpys667.monitor.hud.MonitorOutput
import ru.korpys667.monitor.hud.MonitorOutputCapabilities
import ru.korpys667.monitor.hud.MonitorOutputPolicy
import ru.korpys667.monitor.hud.MonitorRenderContext
import ru.korpys667.monitor.hud.MonitorRenderPayload
import ru.korpys667.monitor.hud.TabListConfig
import ru.korpys667.monitor.hud.fillFrameTemplate

class TabListOutput(private val adventure: BukkitAudiences, private val cache: ComponentCache) :
  MonitorOutput {
  override val kind = MonitorOutputKind.TABLIST

  override val capabilities =
    MonitorOutputCapabilities(
      maxTargets = 1,
      claimsClientSlot = false,
      eventDriven = false,
      requiresClear = true,
    )

  override fun isAvailable(): Boolean = true

  override fun policy(config: MonitorHudRuntimeConfig): MonitorOutputPolicy =
    MonitorOutputPolicy(keepAliveCycles = 0, minIntervalCycles = 0)

  override fun attach(context: MonitorRenderContext): Boolean = true

  override fun render(context: MonitorRenderContext, payload: MonitorRenderPayload) {
    if (!context.viewer.isOnline) {
      return
    }
    val config = context.config.tabList
    val frame = payload.primary
    adventure
      .player(context.viewer)
      .sendPlayerListHeaderAndFooter(
        cache.component(fillFrameTemplate(config.header, frame)),
        cache.component(buildTabListFooter(config, frame)),
      )
  }

  override fun clear(context: MonitorRenderContext) {
    if (!context.viewer.isOnline) {
      return
    }
    adventure
      .player(context.viewer)
      .sendPlayerListHeaderAndFooter(Component.empty(), Component.empty())
  }

  override fun detach(context: MonitorRenderContext) = Unit
}

internal fun buildTabListFooter(config: TabListConfig, frame: MonitorFrame): String {
  val lines = ArrayList<String>(config.footerLines.size)
  for (template in config.footerLines) {
    val line = fillFrameTemplate(template, frame)
    if (line.isNotBlank() && lines.none { line in it }) {
      lines += line
    }
  }
  return lines.joinToString("<newline>")
}
