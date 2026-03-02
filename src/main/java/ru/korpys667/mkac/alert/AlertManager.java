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
package ru.korpys667.mkac.alert;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class AlertManager {

  private final MKAC plugin;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;
  private final BukkitAudiences adventure;

  private final Map<AlertType, Set<UUID>> playersWithAlerts = new EnumMap<>(AlertType.class);

  private final Set<AlertType> consoleAlertsEnabled = EnumSet.allOf(AlertType.class);

  private boolean logToConsole;

  @Getter private String alertFormat;
  @Getter private String brandAlertFormat;

  public AlertManager(
      MKAC plugin,
      ConfigManager configManager,
      LocaleManager localeManager,
      BukkitAudiences adventure) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.localeManager = localeManager;
    this.adventure = adventure;

    for (AlertType type : AlertType.values()) {
      playersWithAlerts.put(type, new CopyOnWriteArraySet<>());
    }

    reload();
  }

  public void reload() {
    this.logToConsole = configManager.getConfig().getBoolean("alerts.print-to-console", true);
    this.alertFormat = localeManager.getRawMessage(Message.ALERTS_FORMAT);
    this.brandAlertFormat = localeManager.getRawMessage(Message.BRAND_NOTIFICATION);
  }

  public void toggle(Player player, AlertType type, boolean silent) {
    Set<UUID> playersSet = playersWithAlerts.get(type);
    UUID uuid = player.getUniqueId();

    if (playersSet.contains(uuid)) {
      playersSet.remove(uuid);
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(type.getDisabledMessage()));
      }
    } else {
      playersSet.add(uuid);
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(type.getEnabledMessage()));
      }
    }
  }

  public void send(Component component, AlertType type) {
    Set<UUID> playersSet = playersWithAlerts.get(type);
    String permission = type.getPermission();

    for (UUID uuid : playersSet) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.hasPermission(permission)) {
        adventure(p).sendMessage(component);
      }
    }

    if (logToConsole && consoleAlertsEnabled.contains(type)) {
      adventure(Bukkit.getConsoleSender()).sendMessage(component);
    }
  }

  public boolean hasAlertsEnabled(Player player, AlertType type) {
    return playersWithAlerts.get(type).contains(player.getUniqueId());
  }

  public boolean isConsoleAlertsEnabled(AlertType type) {
    return consoleAlertsEnabled.contains(type);
  }

  public void toggleConsoleAlerts(AlertType type) {
    if (consoleAlertsEnabled.contains(type)) {
      consoleAlertsEnabled.remove(type);
    } else {
      consoleAlertsEnabled.add(type);
    }
  }

  public void handlePlayerQuit(Player player) {
    UUID uuid = player.getUniqueId();
    for (Set<UUID> players : playersWithAlerts.values()) {
      players.remove(uuid);
    }
  }

  private Audience adventure(Player player) {
    return adventure.player(player);
  }

  private Audience adventure(CommandSender sender) {
    return adventure.sender(sender);
  }
}
