/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import ru.korpys667.alert.AlertManager
import ru.korpys667.alert.AlertType
import ru.korpys667.command.MKACCommand
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

class BrandsCommand(private val alertManager: AlertManager) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("brands").permission("mkac.brand").handler(this@BrandsCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val nativeSender: CommandSender = context.sender().nativeSender

    if (nativeSender is Player) {
      alertManager.toggle(nativeSender, AlertType.BRAND, false)
    } else {
      alertManager.toggleConsoleAlerts(AlertType.BRAND)
      if (alertManager.isConsoleAlertsEnabled(AlertType.BRAND)) {
        MessageUtil.sendMessage(nativeSender, Message.BRAND_ALERTS_ENABLED)
      } else {
        MessageUtil.sendMessage(nativeSender, Message.BRAND_ALERTS_DISABLED)
      }
    }
  }
}
