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
package ru.korpys667.mkac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.command.CommandManager;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.database.DatabaseManager;
import ru.korpys667.mkac.debug.DebugManager;
import ru.korpys667.mkac.event.DamageEvent;
import ru.korpys667.mkac.hologram.HologramManager;
import ru.korpys667.mkac.integration.WorldGuardManager;
import ru.korpys667.mkac.menu.ChickenCoopMenu;
import ru.korpys667.mkac.packet.PacketListener;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.server.AIServerProvider;
import ru.korpys667.mkac.server.StatsReporter;
import ru.korpys667.mkac.utils.MessageUtil;

public final class MKAC extends JavaPlugin {
  private ConfigManager configManager;
  private LocaleManager localeManager;
  private AIServerProvider aiServerProvider;
  private WorldGuardManager worldGuardManager;
  private CommandManager commandManager;
  private AlertManager alertManager;
  private StatsReporter statsReporter;
  @Getter PlayerDataManager playerDataManager;
  @Getter DatabaseManager databaseManager;
  @Getter private ChickenCoopMenu chickenCoopMenu;
  @Getter private HologramManager hologramManager;
  @Getter private DebugManager debugManager;
  @Getter private BukkitAudiences adventure;

  @Override
  public void onLoad() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(true);
    PacketEvents.getAPI().load();
  }

  @Override
  public void onEnable() {
    this.adventure = BukkitAudiences.create(this);

    this.configManager = new ConfigManager(this);
    this.localeManager = new LocaleManager(this, configManager);
    this.debugManager = new DebugManager(this, configManager);

    MessageUtil.init(this.localeManager, this.adventure);

    this.databaseManager = new DatabaseManager(this, configManager);
    this.worldGuardManager = new WorldGuardManager(this, configManager);
    this.alertManager = new AlertManager(this, configManager, localeManager, adventure);
    this.aiServerProvider = new AIServerProvider(this, configManager);
    this.statsReporter = new StatsReporter(this, configManager);
    this.chickenCoopMenu = new ChickenCoopMenu(this);
    this.hologramManager = new HologramManager(this);
    this.playerDataManager =
        new PlayerDataManager(
            this,
            alertManager,
            configManager,
            databaseManager,
            this.aiServerProvider,
            worldGuardManager);

    PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListener(this.playerDataManager));
    PacketEvents.getAPI().init();

    this.commandManager =
        new CommandManager(
            this, alertManager, databaseManager, configManager, localeManager, playerDataManager);

    getServer().getPluginManager().registerEvents(new DamageEvent(playerDataManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(
            new ru.korpys667.mkac.listener.MenuClickListener(this, chickenCoopMenu), this);
  }

  public void reloadPlugin() {
    configManager.reloadConfig();
    localeManager.reload();
    debugManager.reload();
    alertManager.reload();
    statsReporter.reload();

    aiServerProvider.reload();

    if (playerDataManager != null) {
      playerDataManager.reloadAllPlayers();
    }
  }

  @Override
  public void onDisable() {
    if (statsReporter != null) {
      statsReporter.shutdown();
    }
    if (hologramManager != null) {
      hologramManager.shutdown();
    }
    if (this.adventure != null) {
      this.adventure.close();
      this.adventure = null;
    }
    if (databaseManager != null) {
      databaseManager.shutdown();
    }
    if (PacketEvents.getAPI().isInitialized()) {
      PacketEvents.getAPI().terminate();
    }
  }

  public StatsReporter getStatsReporter() {
    return statsReporter;
  }
}
