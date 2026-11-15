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

import org.bukkit.OfflinePlayer
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import ru.korpys667.command.MKACCommand
import ru.korpys667.database.DatabaseManager
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

class PunishCommand(private val databaseManager: DatabaseManager) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("punish")
        .permission("mkac.punish.manage")
        .literal("reset")
        .required("target", OfflinePlayerParser.offlinePlayerParser())
        .handler(this@PunishCommand::reset)
    }
  }

  private fun reset(context: CommandContext<Sender>) {
    val sender = context.sender()
    val target: OfflinePlayer = context["target"]

    if (!databaseManager.isAvailable) {
      MessageUtil.sendMessage(sender.nativeSender, Message.STORAGE_DEGRADED)
    }

    databaseManager.database.resetAllViolationLevels(target.uniqueId)

    MessageUtil.sendMessage(
      sender.nativeSender,
      Message.PUNISH_RESET_SUCCESS,
      "player",
      target.name ?: target.uniqueId.toString(),
    )
  }
}
