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
package ru.korpys667.mkac.command.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.command.CommandRegister;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.command.requirements.PlayerSenderRequirement;
import ru.korpys667.mkac.menu.HistoryMenu;
import ru.korpys667.mkac.sender.Sender;

public class HistoryCommand implements MKCommand {

  private final HistoryMenu historyMenu;

  public HistoryCommand(HistoryMenu historyMenu) {
    this.historyMenu = historyMenu;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("history", "hist")
            .permission("mkac.history")
            .required("target", OfflinePlayerParser.offlinePlayerParser())
            .apply(
                CommandRegister.REQUIREMENT_FACTORY.create(
                    PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
            .handler(this::handleHistory));
  }

  private void handleHistory(CommandContext<Sender> context) {
    Player viewer = context.sender().getPlayer();
    OfflinePlayer target = context.get("target");

    historyMenu.open(viewer, target.getName(), target.getUniqueId(), 1);
  }
}
