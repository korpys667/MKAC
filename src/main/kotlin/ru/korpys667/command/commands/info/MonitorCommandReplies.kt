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

import org.bukkit.entity.Player
import ru.korpys667.monitor.core.MonitorSettings
import ru.korpys667.monitor.hud.MonitorHudService
import ru.korpys667.monitor.hud.MonitorTargetsService
import ru.korpys667.monitor.hud.TargetChange
import ru.korpys667.monitor.hud.outputCapacity
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

internal fun restartIfSessionShapeChanged(
  hudService: MonitorHudService,
  player: Player,
  before: MonitorSettings,
  updated: MonitorSettings,
) {
  if (before.outputs != updated.outputs || before.chatStyle != updated.chatStyle) {
    hudService.restart(player.uniqueId)
  }
}

internal fun replyToAdd(
  viewer: Player,
  target: Player,
  change: TargetChange,
  targets: MonitorTargetsService,
  hudService: MonitorHudService,
) {
  when (change) {
    TargetChange.APPLIED -> replyAdded(viewer, target, targets, hudService)
    TargetChange.ALREADY_WATCHED ->
      MessageUtil.sendMessage(viewer, Message.MONITOR_TARGET_ALREADY, "player", target.name)
    else ->
      MessageUtil.sendMessage(
        viewer,
        Message.MONITOR_TARGET_LIMIT,
        "output",
        hudService.session(viewer.uniqueId)?.outputs?.joinToString(", ") { it.kind.key }.orEmpty(),
        "limit",
        targets.capacity(viewer.uniqueId).toString(),
      )
  }
}

private fun replyAdded(
  viewer: Player,
  target: Player,
  targets: MonitorTargetsService,
  hudService: MonitorHudService,
) {
  val total = targets.size(viewer.uniqueId)
  MessageUtil.sendMessage(
    viewer,
    Message.MONITOR_TARGET_ADDED,
    "player",
    target.name,
    "count",
    total.toString(),
  )
  val session = hudService.session(viewer.uniqueId) ?: return
  val narrowest = session.outputs.minByOrNull { outputCapacity(it, session.config) } ?: return
  val shown = outputCapacity(narrowest, session.config)
  if (total > shown) {
    MessageUtil.sendMessage(
      viewer,
      Message.MONITOR_TARGET_OVERFLOW,
      "output",
      narrowest.kind.key,
      "shown",
      shown.toString(),
    )
  }
}
