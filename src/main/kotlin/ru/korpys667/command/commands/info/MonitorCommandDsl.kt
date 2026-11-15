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
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.suggestionProvider
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider
import ru.korpys667.command.CommandRegister
import ru.korpys667.command.requirements.PlayerSenderRequirement
import ru.korpys667.monitor.core.MonitorTargetMode
import ru.korpys667.sender.Sender

internal const val MONITOR_SELF_PERMISSION = "mkac.monitor.self"
internal const val MONITOR_PARENT_PERMISSION = "mkac.monitor"

internal fun monitorCommand(
  manager: CommandManager<Sender>,
  path: List<String> = emptyList(),
  permission: String = MONITOR_SELF_PERMISSION,
  playerOnly: Boolean = true,
  configure: MutableCommandBuilder<Sender>.() -> Unit,
) {
  // "prob" is a legacy alias for "monitor" and must stay in full parity with it.
  listOf("monitor", "prob").forEach { root ->
    manager.buildAndRegister("mkac") {
      literal(root).permission(permission)
      if (playerOnly) {
        mutate { it.apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement)) }
      }
      path.forEach { literal(it) }
      configure.invoke(this)
    }
  }
}

internal fun registerMonitorSetting(
  manager: CommandManager<Sender>,
  key: String,
  argument: String,
  suggestions: SuggestionProvider<Sender>,
  handler: (CommandContext<Sender>) -> Unit,
) {
  listOf(listOf("set", key), listOf(key)).forEach { path ->
    monitorCommand(manager, path = path) {
      required(argument, StringParser.stringParser()) { suggestionProvider = suggestions }
      handler(handler)
    }
  }
}

internal fun canWatchOthers(viewer: Player): Boolean =
  viewer.hasPermission("mkac.monitor.others") || viewer.hasPermission(MONITOR_PARENT_PERMISSION)

internal fun canWatchMany(viewer: Player): Boolean =
  viewer.hasPermission("mkac.monitor.multi") || viewer.hasPermission(MONITOR_PARENT_PERMISSION)

internal fun canWatchAuto(viewer: Player, mode: MonitorTargetMode): Boolean =
  canWatchOthers(viewer) &&
    canWatchMany(viewer) &&
    (viewer.hasPermission("mkac.monitor.${mode.key}") ||
      viewer.hasPermission(MONITOR_PARENT_PERMISSION))
