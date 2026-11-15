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
package ru.korpys667.command.commands.admin

import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import ru.korpys667.MKAC
import ru.korpys667.command.MKACCommand
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

class ReloadCommand(private val plugin: MKAC) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("reload").permission("mkac.reload").handler(this@ReloadCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    MessageUtil.sendMessage(context.sender().nativeSender, Message.RELOAD_START)
    plugin.onReload()
    MessageUtil.sendMessage(context.sender().nativeSender, Message.RELOAD_SUCCESS)
  }
}
