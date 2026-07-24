package ru.korpys667.mkac.alert;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.redis.CrossServerPublisher;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class AlertManager {

  private final MKAC plugin;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;

  private final Map<AlertType, Set<UUID>> playersWithAlerts = new EnumMap<>(AlertType.class);
  private final Set<AlertType> consoleAlertsEnabled = EnumSet.allOf(AlertType.class);

  private boolean logToConsole;

  @Getter private String alertFormat;
  @Getter private String brandAlertFormat;

  private volatile CrossServerPublisher crossServerPublisher;

  public void setCrossServerPublisher(CrossServerPublisher publisher) {
    this.crossServerPublisher = publisher;
  }

  public AlertManager(MKAC plugin, ConfigManager configManager, LocaleManager localeManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.localeManager = localeManager;

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
        player.sendMessage(MessageUtil.getMessage(type.getDisabledMessage()));
      }
    } else {
      playersSet.add(uuid);
      if (!silent) {
        player.sendMessage(MessageUtil.getMessage(type.getEnabledMessage()));
      }
    }
  }

  public void send(String message, AlertType type) {
    deliver(message, type);
    CrossServerPublisher publisher = this.crossServerPublisher;
    if (publisher != null) {
      publisher.publish(type, message);
    }
  }

  public void deliver(String message, AlertType type) {
    Set<UUID> playersSet = playersWithAlerts.get(type);
    String permission = type.getPermission();

    for (UUID uuid : playersSet) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.hasPermission(permission)) {
        p.sendMessage(message);
      }
    }

    if (logToConsole && consoleAlertsEnabled.contains(type)) {
      Bukkit.getConsoleSender().sendMessage(message);
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
}
