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
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

class MonitorLifecycle(
  private val hudService: MonitorHudService,
  private val index: MonitorTargetIndex,
) : Listener {
  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) {
    val player = event.player
    hudService.stop(player.uniqueId, player)
    dropWatchers(player)
  }

  private fun dropWatchers(target: Player) {
    val sessions =
      index
        .viewersOf(target.uniqueId)
        .mapNotNull { viewerId -> hudService.session(viewerId)?.let { viewerId to it } }
        .filterNot { (_, session) -> session.targetMode.isAuto }
    for ((viewerId, session) in sessions) {
      session.targets.remove(target.uniqueId)
      if (session.targets.size == 0) {
        hudService.stop(viewerId, session.viewer)
      } else {
        index.set(viewerId, session.targets.ids())
      }
      MessageUtil.sendMessage(session.viewer, Message.MONITOR_DISABLED, "player", target.name)
    }
  }
}
