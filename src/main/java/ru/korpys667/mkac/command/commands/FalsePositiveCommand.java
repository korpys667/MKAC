/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
 *
 * This file contains code derived from MLSAC (GPLv3).
 * Original authors: SoMax1soft, MLSAC.NET project.
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

import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.checks.impl.ai.AICheck;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.data.TickData;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.server.DataRestorer;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class FalsePositiveCommand implements MKCommand {

  private final MKAC plugin;
  private final PlayerDataManager playerDataManager;
  private final DataRestorer dataRestorer;

  public FalsePositiveCommand(MKAC plugin, PlayerDataManager playerDataManager) {
    this.plugin = plugin;
    this.playerDataManager = playerDataManager;
    this.dataRestorer = new DataRestorer(plugin);
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("falsepositive", "fp")
            .permission("mkac.falsepositive")
            .literal("restore")
            .required("target", org.incendo.cloud.bukkit.parser.PlayerParser.playerParser())
            .handler(this::handleFalsePositive));
  }

  private void handleFalsePositive(CommandContext<Sender> context) {
    Sender sender = context.sender();
    Player target = context.get("target");

    MKPlayer mkPlayer = playerDataManager.getPlayer(target);
    if (mkPlayer == null) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.FP_NO_DATA);
      return;
    }

    AICheck aiCheck = mkPlayer.getCheckManager().getCheck(AICheck.class);
    if (aiCheck == null) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.FP_NO_DATA);
      return;
    }

    java.util.List<TickData> history = aiCheck.getTickHistory();
    if (history.isEmpty()) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.FP_NO_DATA);
      return;
    }

    boolean success = dataRestorer.restoreData(target.getName(), history);

    if (success) {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.FP_SUCCESS, "player", target.getName());
    } else {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.FP_FAIL);
    }
  }
}
