/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
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
package ru.korpys667.mkac.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class AlertsCommand implements MKCommand {

  private final AlertManager alertManager;

  public AlertsCommand(AlertManager alertManager) {
    this.alertManager = alertManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("alerts")
            .permission("mkac.alerts")
            .handler(this::execute));
  }

  private void execute(CommandContext<Sender> context) {
    CommandSender nativeSender = context.sender().getNativeSender();

    if (nativeSender instanceof Player player) {
      alertManager.toggle(player, AlertType.REGULAR, false);
    } else {
      alertManager.toggleConsoleAlerts(AlertType.REGULAR);
      if (alertManager.isConsoleAlertsEnabled(AlertType.REGULAR)) {
        MessageUtil.sendMessage(nativeSender, Message.ALERTS_ENABLED);
      } else {
        MessageUtil.sendMessage(nativeSender, Message.ALERTS_DISABLED);
      }
    }
  }
}
