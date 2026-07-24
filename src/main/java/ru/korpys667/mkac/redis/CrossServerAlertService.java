package ru.korpys667.mkac.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class CrossServerAlertService implements CrossServerPublisher {

  private static final String DEFAULT_SERVER_NAME = "server-1";
  private static final String DEFAULT_CHANNEL = "mkac:alerts";

  private final ConfigManager configManager;
  private final RedisManager redisManager;
  private final AlertManager alertManager;
  private final MKAC plugin;
  private final Logger logger;

  private final String origin = UUID.randomUUID().toString();
  private final ObjectMapper mapper = new ObjectMapper();

  private boolean enabled = false;
  private Set<AlertType> mirroredTypes = EnumSet.noneOf(AlertType.class);
  private String serverName = DEFAULT_SERVER_NAME;
  private String channel = DEFAULT_CHANNEL;

  public CrossServerAlertService(
      ConfigManager configManager,
      RedisManager redisManager,
      AlertManager alertManager,
      MKAC plugin,
      Logger logger) {
    this.configManager = configManager;
    this.redisManager = redisManager;
    this.alertManager = alertManager;
    this.plugin = plugin;
    this.logger = logger;
  }

  public void start() {
    if (!configManager.getConfig().getBoolean("cross-server.enabled", false)) return;

    serverName =
        configManager.getConfig().getString("cross-server.server-name", DEFAULT_SERVER_NAME);
    channel = configManager.getConfig().getString("cross-server.channel", DEFAULT_CHANNEL);

    if (configManager.getConfig().getBoolean("cross-server.alerts.regular", true)) {
      mirroredTypes.add(AlertType.REGULAR);
    }
    if (configManager.getConfig().getBoolean("cross-server.alerts.suspicious", true)) {
      mirroredTypes.add(AlertType.SUSPICIOUS);
    }

    if (mirroredTypes.isEmpty()) {
      logger.info("[CrossServer] No alert types selected for mirroring, cross-server disabled.");
      return;
    }

    redisManager.start();
    if (!redisManager.isAvailable()) {
      logger.warning("[CrossServer] Redis unavailable; cross-server alerts disabled.");
      return;
    }

    enabled = true;

    redisManager.subscribe(channel, this::onMessage);

    alertManager.setCrossServerPublisher(this);

    logger.info(
        "[CrossServer] Enabled ("
            + serverName
            + "), mirroring: "
            + mirroredTypes
            + " on channel: "
            + channel);
  }

  @Override
  public void publish(AlertType type, String message) {
    if (!enabled || !mirroredTypes.contains(type)) return;

    try {
      CrossServerAlert alert = new CrossServerAlert(origin, serverName, type.name(), message);
      String payload = mapper.writeValueAsString(alert);
      redisManager.publishAsync(channel, payload);
    } catch (Exception e) {
      logger.log(Level.FINE, "[CrossServer] Failed to publish alert", e);
    }
  }

  private void onMessage(String raw) {
    try {
      CrossServerAlert alert = mapper.readValue(raw, CrossServerAlert.class);

      if (alert.getOrigin().equals(origin)) return;

      AlertType type = AlertType.valueOf(alert.getType());
      if (!mirroredTypes.contains(type)) return;

      String prefixed =
          MessageUtil.getMessage(Message.CROSS_SERVER_ALERT_PREFIX, "server", alert.getServer())
              + " "
              + alert.getComponent();

      plugin.getServer().getScheduler().runTask(plugin, () -> alertManager.deliver(prefixed, type));

    } catch (Exception e) {
      logger.log(Level.FINE, "[CrossServer] Failed to process incoming alert", e);
    }
  }

  public void shutdown() {
    enabled = false;
    alertManager.setCrossServerPublisher(null);
  }
}
