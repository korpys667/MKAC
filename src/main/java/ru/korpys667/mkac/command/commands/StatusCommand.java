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
package ru.korpys667.mkac.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.hologram.HologramManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class StatusCommand implements MKCommand {

  private final HologramManager hologramManager;

  public StatusCommand(HologramManager hologramManager) {
    this.hologramManager = hologramManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("status")
            .permission("mkac.status")
            .handler(this::executeAll));

    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("status")
            .permission("mkac.status")
            .required("target", PlayerParser.playerParser())
            .handler(this::executeTarget));
  }

  private void executeAll(CommandContext<Sender> context) {
    CommandSender sender = context.sender().getNativeSender();

    if (!(sender instanceof Player player)) {
      MessageUtil.sendMessage(sender, Message.RUN_AS_PLAYER);
      return;
    }

    boolean hasAny = false;
    for (Player target : org.bukkit.Bukkit.getOnlinePlayers()) {
      if (hologramManager.isEnabled(player, target)) {
        hasAny = true;
        break;
      }
    }

    if (hasAny) {
      hologramManager.disableForAll(player);
      player.sendMessage(ChatColor.YELLOW + "Голограммы отключены для всех игроков.");
    } else {
      hologramManager.enableForAll(player);
      player.sendMessage(ChatColor.GREEN + "Голограммы включены для всех игроков.");
    }
  }

  private void executeTarget(CommandContext<Sender> context) {
    CommandSender sender = context.sender().getNativeSender();

    if (!(sender instanceof Player player)) {
      MessageUtil.sendMessage(sender, Message.RUN_AS_PLAYER);
      return;
    }

    Player target = context.get("target");

    if (target.hasPermission("mkac.exempt")) {
      player.sendMessage(ChatColor.RED + "Этот игрок освобожден от проверок.");
      return;
    }

    if (hologramManager.isEnabled(player, target)) {
      hologramManager.disableHologram(player, target);
      player.sendMessage(
          ChatColor.YELLOW
              + "Голограмма отключена для игрока "
              + ChatColor.WHITE
              + target.getName());
    } else {
      hologramManager.enableHologram(player, target);
      player.sendMessage(
          ChatColor.GREEN + "Голограмма включена для игрока " + ChatColor.WHITE + target.getName());
    }
  }
}
