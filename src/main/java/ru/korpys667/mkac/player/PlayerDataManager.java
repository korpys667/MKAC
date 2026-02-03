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
package ru.korpys667.mkac.player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.database.DatabaseManager;
import ru.korpys667.mkac.integration.WorldGuardManager;
import ru.korpys667.mkac.server.AIServerProvider;

public class PlayerDataManager implements Listener {
  private final MKAC plugin;
  private final AlertManager alertManager;
  private final ConfigManager configManager;
  private final DatabaseManager databaseManager;
  private final WorldGuardManager worldGuardManager;
  private AIServerProvider aiServerProvider;

  private final Map<UUID, MKPlayer> players = new ConcurrentHashMap<>();

  public PlayerDataManager(
      MKAC plugin,
      AlertManager alertManager,
      ConfigManager configManager,
      DatabaseManager databaseManager,
      AIServerProvider aiServerProvider,
      WorldGuardManager worldGuardManager) {
    this.plugin = plugin;
    this.alertManager = alertManager;
    this.configManager = configManager;
    this.databaseManager = databaseManager;
    this.aiServerProvider = aiServerProvider;
    this.worldGuardManager = worldGuardManager;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (player.hasPermission("mkac.exempt")) {
      return;
    }

    players.put(
        player.getUniqueId(),
        new MKPlayer(
            player,
            plugin,
            configManager,
            databaseManager,
            alertManager,
            aiServerProvider,
            worldGuardManager));

    plugin.getChickenCoopMenu().restorePlayer(player.getUniqueId(), player.getName());

    if (player.hasPermission("mkac.alerts") && player.hasPermission("mkac.alerts.enable-on-join")) {
      if (!alertManager.hasAlertsEnabled(player, AlertType.REGULAR)) {
        alertManager.toggle(player, AlertType.REGULAR, true);
      }
    }

    if (player.hasPermission("mkac.brand") && player.hasPermission("mkac.brand.enable-on-join")) {
      if (!alertManager.hasAlertsEnabled(player, AlertType.BRAND)) {
        alertManager.toggle(player, AlertType.BRAND, true);
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = event.getPlayer().getUniqueId();

    plugin.getChickenCoopMenu().removePlayer(player.getUniqueId());
    plugin.getHologramManager().handlePlayerQuit(player);

    alertManager.handlePlayerQuit(player);
    players.remove(uuid);
  }

  public MKPlayer getPlayer(Player player) {
    if (player == null) {
      return null;
    }
    return players.get(player.getUniqueId());
  }

  public MKPlayer getPlayer(UUID uuid) {
    return players.get(uuid);
  }

  public Collection<MKPlayer> getPlayers() {
    return players.values();
  }

  public void reloadAllPlayers() {
    for (MKPlayer mkPlayer : players.values()) {
      mkPlayer.reload();
    }
  }
}
