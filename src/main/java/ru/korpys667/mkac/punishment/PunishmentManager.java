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
package ru.korpys667.mkac.punishment;

import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.checks.ICheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.database.ViolationDatabase;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class PunishmentManager {
  private final MKPlayer mkPlayer;
  private final MKAC plugin;
  private final ConfigManager configManager;
  private final Map<String, PunishGroup> punishmentGroups = new HashMap<>();
  private final AlertManager alertManager;
  private final ViolationDatabase database;

  public PunishmentManager(
      MKPlayer mkPlayer,
      MKAC plugin,
      ConfigManager configManager,
      ViolationDatabase database,
      AlertManager alertManager) {
    this.mkPlayer = mkPlayer;
    this.plugin = plugin;
    this.configManager = configManager;
    this.alertManager = alertManager;
    this.database = database;
    reload();
  }

  public void reload() {
    punishmentGroups.clear();

    ConfigurationSection punishmentsSection =
        configManager.getPunishments().getConfigurationSection("Punishments");
    if (punishmentsSection == null) return;

    for (String groupName : punishmentsSection.getKeys(false)) {
      ConfigurationSection groupSection = punishmentsSection.getConfigurationSection(groupName);
      if (groupSection == null) continue;

      List<String> checkNamesFilters = groupSection.getStringList("checks");
      ConfigurationSection actionsSection = groupSection.getConfigurationSection("actions");

      if (actionsSection == null) continue;

      NavigableMap<Integer, List<String>> parsedActions = new TreeMap<>();
      for (String vlString : actionsSection.getKeys(false)) {
        try {
          int vl = Integer.parseInt(vlString);
          List<String> commands = actionsSection.getStringList(vlString);
          parsedActions.put(vl, commands);
        } catch (NumberFormatException e) {
          plugin
              .getLogger()
              .warning("Invalid VL " + vlString + " in punishment group " + groupName + ".");
        }
      }

      if (!parsedActions.isEmpty()) {
        PunishGroup punishGroup = new PunishGroup(groupName, checkNamesFilters, parsedActions);
        punishmentGroups.put(groupName, punishGroup);
      }
    }
  }

  public void handleFlag(ICheck check, String debug) {
    for (PunishGroup group : punishmentGroups.values()) {
      if (group.isCheckAssociated(check)) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(
                plugin,
                () -> {
                  int newVl =
                      database.incrementViolationLevel(mkPlayer.getUuid(), group.getGroupName());

                  Map.Entry<Integer, List<String>> entry = group.getActions().floorEntry(newVl);
                  if (entry != null) {
                    executeCommands(check, group, newVl, debug, entry.getValue());
                  }
                });
      }
    }
  }

  private void executeCommands(
      ICheck check, PunishGroup group, int vl, String verbose, List<String> commands) {
    for (String command : commands) {
      String commandLower = command.toLowerCase(Locale.ROOT);

      if (commandLower.equals("[alert]")) {
        sendAlert(check, vl, verbose);
      } else if (commandLower.equals("[log]")) {
        database.logAlert(mkPlayer, verbose, check.getCheckName(), vl);
      } else if (commandLower.equals("[reset]")) {
        database.resetViolationLevel(mkPlayer.getUuid(), group.getGroupName());
      } else if (commandLower.startsWith("[broadcast] ")) {
        final String message = command.substring("[broadcast] ".length());
        final Component component =
            MessageUtil.format(
                message,
                "player",
                mkPlayer.getPlayer().getName(),
                "check_name",
                check.getCheckName(),
                "vl",
                String.valueOf(vl),
                "verbose",
                verbose);

        Bukkit.getScheduler()
            .runTask(
                plugin,
                () -> {
                  plugin.getAdventure().players().sendMessage(component);
                });
      } else {
        String formattedCmd =
            command
                .replace("<player>", mkPlayer.getPlayer().getName())
                .replace("<check_name>", check.getCheckName())
                .replace("<vl>", String.valueOf(vl))
                .replace("<verbose>", verbose);

        Bukkit.getScheduler()
            .runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd));
      }
    }
  }

  private void sendAlert(ICheck check, int vl, String verbose) {
    final Component message =
        MessageUtil.getMessage(
            Message.ALERTS_FORMAT,
            "player",
            mkPlayer.getPlayer().getName(),
            "check_name",
            check.getCheckName(),
            "vl",
            String.valueOf(vl),
            "verbose",
            verbose);

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              alertManager.send(message, AlertType.REGULAR);
            });
  }
}
