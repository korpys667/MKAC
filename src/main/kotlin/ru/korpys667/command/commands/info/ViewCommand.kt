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

import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import ru.korpys667.command.CommandRegister
import ru.korpys667.command.MKACCommand
import ru.korpys667.command.requirements.PlayerSenderRequirement
import ru.korpys667.monitor.view.MonitorViewService
import ru.korpys667.monitor.view.VIEW_PERMISSION
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

class ViewCommand(private val monitorViewService: MonitorViewService) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("view")
        .permission(VIEW_PERMISSION)
        .mutate { it.apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement)) }
        .handler(this@ViewCommand::toggle)
    }
  }

  private fun toggle(context: CommandContext<Sender>) {
    val viewer = context.sender().player ?: return
    val enabled = monitorViewService.toggle(viewer)

    if (enabled) {
      MessageUtil.sendMessage(viewer, Message.VIEW_ENABLED)
    } else {
      MessageUtil.sendMessage(viewer, Message.VIEW_DISABLED)
    }
  }
}
