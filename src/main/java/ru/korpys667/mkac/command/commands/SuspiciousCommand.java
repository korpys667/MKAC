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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.DoubleParser;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.checks.impl.ai.AICheck;
import ru.korpys667.mkac.command.CommandRegister;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.command.requirements.PlayerSenderRequirement;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class SuspiciousCommand implements MKCommand {
  private final PlayerDataManager playerDataManager;
  private final AlertManager alertManager;

  public SuspiciousCommand(PlayerDataManager playerDataManager, AlertManager alertManager) {
    this.playerDataManager = playerDataManager;
    this.alertManager = alertManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var base =
        manager.commandBuilder("mkac").literal("suspicious").permission("mkac.suspicious");

    manager.command(
        base.literal("alerts")
            .permission("mkac.suspicious.alerts")
            .apply(
                CommandRegister.REQUIREMENT_FACTORY.create(
                    PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
            .handler(this::executeAlerts));

    manager.command(
        base.literal("list")
            .permission("mkac.suspicious.list")
            .flag(manager.flagBuilder("buffer").withComponent(DoubleParser.doubleParser(0.0)))
            .handler(this::executeList));

    manager.command(
        base.literal("top").permission("mkac.suspicious.top").handler(this::executeTop));
  }

  private void executeAlerts(CommandContext<Sender> context) {
    final Player player = context.sender().getPlayer();
    alertManager.toggle(player, AlertType.SUSPICIOUS, false);
  }

  private void executeList(CommandContext<Sender> context) {
    final Sender sender = context.sender();

    final Double bufferFlag = context.flags().get("buffer");
    final double bufferFilter = bufferFlag != null ? bufferFlag : 0.0;

    List<MKPlayer> suspiciousPlayers =
        playerDataManager.getPlayers().stream()
            .filter(
                sp -> {
                  AICheck check = sp.getCheckManager().getCheck(AICheck.class);
                  return check != null && check.getBuffer() > bufferFilter;
                })
            .sorted(
                Comparator.comparingDouble(
                    sp -> -sp.getCheckManager().getCheck(AICheck.class).getBuffer()))
            .collect(Collectors.toList());

    if (suspiciousPlayers.isEmpty()) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_LIST_EMPTY));
      return;
    }

    sender.sendMessage(
        MessageUtil.getMessage(
            Message.SUSPICIOUS_LIST_HEADER, "count", String.valueOf(suspiciousPlayers.size())));
    for (MKPlayer sp : suspiciousPlayers) {
      AICheck aiCheck = sp.getCheckManager().getCheck(AICheck.class);
      double buffer = aiCheck.getBuffer();
      String playerName = sp.getPlayer().getName();

      Component entry =
          MessageUtil.getMessage(
              Message.SUSPICIOUS_LIST_ENTRY,
              "player",
              playerName,
              "buffer",
              String.format("%.1f", buffer),
              "ping",
              String.valueOf(sp.getPlayer().getPing()));

      sender.sendMessage(entry);
    }
  }

  private void executeTop(CommandContext<Sender> context) {
    final Sender sender = context.sender();

    MKPlayer topPlayer =
        playerDataManager.getPlayers().stream()
            .filter(sp -> sp.getCheckManager().getCheck(AICheck.class) != null)
            .max(
                Comparator.comparingDouble(
                    sp -> sp.getCheckManager().getCheck(AICheck.class).getBuffer()))
            .orElse(null);

    if (topPlayer == null || topPlayer.getCheckManager().getCheck(AICheck.class).getBuffer() == 0) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_TOP_NONE));
      return;
    }

    String playerName = topPlayer.getPlayer().getName();
    double buffer = topPlayer.getCheckManager().getCheck(AICheck.class).getBuffer();

    Component message =
        MessageUtil.getMessage(
            Message.SUSPICIOUS_TOP_PLAYER,
            "player",
            playerName,
            "buffer",
            String.format("%.1f", buffer));

    sender.sendMessage(message);
  }
}
