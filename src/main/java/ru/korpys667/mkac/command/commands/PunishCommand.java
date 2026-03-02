/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
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
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.database.DatabaseManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class PunishCommand implements MKCommand {

  private final DatabaseManager databaseManager;

  public PunishCommand(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var baseBuilder =
        manager.commandBuilder("mkac").literal("punish").permission("mkac.punish.manage");

    manager.command(
        baseBuilder
            .literal("reset")
            .required("target", OfflinePlayerParser.offlinePlayerParser())
            .handler(this::reset));
  }

  private void reset(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final OfflinePlayer target = context.get("target");

    databaseManager.getDatabase().resetAllViolationLevels(target.getUniqueId());

    MessageUtil.sendMessage(
        sender.getNativeSender(), Message.PUNISH_RESET_SUCCESS, "player", target.getName());
  }
}
