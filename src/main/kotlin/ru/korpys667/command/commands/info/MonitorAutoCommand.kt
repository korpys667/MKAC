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
package ru.korpys667.command.commands.info

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.korpys667.monitor.core.MonitorSettingsService
import ru.korpys667.monitor.core.MonitorTargetMode
import ru.korpys667.monitor.hud.MonitorHudService
import ru.korpys667.monitor.hud.StartResult
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

internal class MonitorAutoCommand(
  private val hudService: MonitorHudService,
  private val settingsService: MonitorSettingsService,
) {
  fun toggle(viewer: Player, sender: CommandSender, mode: MonitorTargetMode) {
    when {
      !canWatchAuto(viewer, mode) ->
        MessageUtil.sendMessage(sender, Message.MONITOR_AUTO_NO_PERMISSION, "mode", mode.key)
      hudService.session(viewer.uniqueId)?.targetMode == mode -> stop(viewer, sender, mode)
      else -> start(viewer, sender, mode)
    }
  }

  fun manual(viewer: Player, sender: CommandSender) {
    val session = hudService.session(viewer.uniqueId)
    if (session == null || !session.targetMode.isAuto) {
      MessageUtil.sendMessage(sender, Message.MONITOR_AUTO_NOT_ACTIVE)
      return
    }
    session.targetMode = MonitorTargetMode.MANUAL
    MessageUtil.sendMessage(
      sender,
      Message.MONITOR_AUTO_MANUAL,
      "count",
      session.targets.size.toString(),
    )
  }

  private fun stop(viewer: Player, sender: CommandSender, mode: MonitorTargetMode) {
    hudService.stop(viewer.uniqueId, viewer)
    MessageUtil.sendMessage(sender, Message.MONITOR_AUTO_STOPPED, "mode", mode.key)
  }

  private fun start(viewer: Player, sender: CommandSender, mode: MonitorTargetMode) {
    when (hudService.start(viewer, null, mode)) {
      StartResult.STARTED ->
        MessageUtil.sendMessage(sender, Message.MONITOR_AUTO_ENABLED, "mode", mode.key)
      StartResult.LIMIT_REACHED ->
        MessageUtil.sendMessage(
          sender,
          Message.MONITOR_LIMIT_REACHED,
          "limit",
          hudService.runtimeConfig.limits.maxSessions.toString(),
        )
      StartResult.NO_OUTPUT ->
        MessageUtil.sendMessage(
          sender,
          Message.MONITOR_OUTPUT_DISABLED,
          "output",
          settingsService.getSettings(viewer.uniqueId).outputs.joinToString(", ") { it.key },
          "fallback",
          settingsService.defaults().outputs.joinToString(", ") { it.key },
        )
    }
  }
}
