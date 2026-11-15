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

import ru.korpys667.MKAC
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.monitor.core.MonitorSettingsLifecycle
import ru.korpys667.monitor.core.MonitorSettingsService

class MonitorRuntime(
  private val plugin: MKAC,
  private val settingsService: MonitorSettingsService,
  private val hudService: MonitorHudService,
  private val index: MonitorTargetIndex,
  private val liveChatListener: MonitorLiveChatListener,
  private val eventBus: MKACEventBus,
) {
  fun enable() {
    plugin.server.pluginManager.registerEvents(MonitorSettingsLifecycle(settingsService), plugin)
    plugin.server.pluginManager.registerEvents(MonitorLifecycle(hudService, index), plugin)
    plugin.server.onlinePlayers.forEach { settingsService.prewarm(it.uniqueId) }
    liveChatListener.register(eventBus, plugin)
  }

  fun disable() {
    hudService.stopAll()
  }

  fun reload() {
    settingsService.reload()
    hudService.reload()
  }
}
