package ru.korpys667.mkac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
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
import ru.korpys667.mkac.menu.HistoryMenu;
import ru.korpys667.mkac.packet.PacketListener;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.redis.CrossServerAlertService;
import ru.korpys667.mkac.redis.CrossServerSuspiciousService;
import ru.korpys667.mkac.redis.RedisManager;
import ru.korpys667.mkac.server.AIServerProvider;
import ru.korpys667.mkac.server.StatsReporter;
import ru.korpys667.mkac.utils.MessageUtil;

public final class MKAC extends JavaPlugin {
  private LocaleManager localeManager;
  private AIServerProvider aiServerProvider;
  private WorldGuardManager worldGuardManager;
  private CommandManager commandManager;
  private AlertManager alertManager;
  private StatsReporter statsReporter;
  @Getter PlayerDataManager playerDataManager;
  @Getter DatabaseManager databaseManager;
  @Getter private ConfigManager configManager;
  @Getter private ChickenCoopMenu chickenCoopMenu;
  @Getter private HistoryMenu historyMenu;
  @Getter private HologramManager hologramManager;
  @Getter private DebugManager debugManager;
  private RedisManager redisManager;
  private CrossServerAlertService crossServerAlertService;
  private CrossServerSuspiciousService crossServerSuspiciousService;

  @Override
  public void onLoad() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(true);
    PacketEvents.getAPI().load();
  }

  @Override
  public void onEnable() {
    this.configManager = new ConfigManager(this);
    this.localeManager = new LocaleManager(this, configManager);
    this.debugManager = new DebugManager(this, configManager);

    MessageUtil.init(this.localeManager);

    this.databaseManager = new DatabaseManager(this, configManager);
    this.worldGuardManager = new WorldGuardManager(this, configManager);

    this.alertManager = new AlertManager(this, configManager, localeManager);

    this.aiServerProvider = new AIServerProvider(this, configManager);
    this.statsReporter = new StatsReporter(this, configManager);
    this.chickenCoopMenu = new ChickenCoopMenu(this);
    this.historyMenu = new HistoryMenu(this);
    this.hologramManager = new HologramManager(this);
    this.playerDataManager =
        new PlayerDataManager(
            this,
            alertManager,
            configManager,
            databaseManager,
            this.aiServerProvider,
            worldGuardManager);

    PacketEvents.getAPI().init();
    PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListener(this.playerDataManager));

    this.redisManager = new RedisManager(configManager, getLogger());
    this.crossServerAlertService =
        new CrossServerAlertService(
            configManager, this.redisManager, alertManager, this, getLogger());
    this.crossServerAlertService.start();
    this.crossServerSuspiciousService =
        new CrossServerSuspiciousService(
            configManager, this.redisManager, playerDataManager, this, getLogger());
    this.crossServerSuspiciousService.start();

    this.commandManager =
        new CommandManager(
            this,
            alertManager,
            databaseManager,
            configManager,
            localeManager,
            playerDataManager,
            historyMenu);

    getServer().getPluginManager().registerEvents(new DamageEvent(playerDataManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(
            new ru.korpys667.mkac.listener.MenuClickListener(this, chickenCoopMenu, historyMenu),
            this);
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
    if (databaseManager != null) {
      databaseManager.shutdown();
    }
    if (PacketEvents.getAPI().isInitialized()) {
      PacketEvents.getAPI().terminate();
    }
    if (crossServerSuspiciousService != null) {
      crossServerSuspiciousService.shutdown();
    }
    if (crossServerAlertService != null) {
      crossServerAlertService.shutdown();
    }
    if (redisManager != null) {
      redisManager.shutdown();
    }
  }

  public StatsReporter getStatsReporter() {
    return statsReporter;
  }
}
