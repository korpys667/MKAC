package ru.korpys667.mkac.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.checks.impl.ai.AICheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;

public class CrossServerSuspiciousService {

  private static final String DEFAULT_SERVER_NAME = "server-1";
  private static final String DEFAULT_CHANNEL = "mkac:alerts";
  private static final long DEFAULT_TTL_SECONDS = 30L;
  private static final long DEFAULT_REFRESH_SECONDS = 10L;

  private final ConfigManager configManager;
  private final RedisManager redisManager;
  private final PlayerDataManager playerDataManager;
  private final MKAC plugin;
  private final Logger logger;

  private final ObjectMapper mapper = new ObjectMapper();

  private boolean enabled = false;
  private String serverName = DEFAULT_SERVER_NAME;
  private String keyPrefix = DEFAULT_CHANNEL + ":suspect";
  private long ttlSeconds = DEFAULT_TTL_SECONDS;
  private int taskId = -1;

  public CrossServerSuspiciousService(
      ConfigManager configManager,
      RedisManager redisManager,
      PlayerDataManager playerDataManager,
      MKAC plugin,
      Logger logger) {
    this.configManager = configManager;
    this.redisManager = redisManager;
    this.playerDataManager = playerDataManager;
    this.plugin = plugin;
    this.logger = logger;
  }

  public boolean isActive() {
    return enabled;
  }

  public void start() {
    if (!configManager.getConfig().getBoolean("cross-server.enabled", false)
        || !configManager.getConfig().getBoolean("cross-server.alerts.suspicious", true)) {
      return;
    }

    serverName =
        configManager.getConfig().getString("cross-server.server-name", DEFAULT_SERVER_NAME);
    String channel = configManager.getConfig().getString("cross-server.channel", DEFAULT_CHANNEL);
    keyPrefix = channel + ":suspect";
    ttlSeconds =
        configManager
            .getConfig()
            .getLong("cross-server.suspicious-sync.ttl-seconds", DEFAULT_TTL_SECONDS);
    long refreshSeconds =
        Math.max(
            1L,
            Math.min(
                configManager
                    .getConfig()
                    .getLong(
                        "cross-server.suspicious-sync.refresh-seconds", DEFAULT_REFRESH_SECONDS),
                ttlSeconds));
    ttlSeconds = Math.max(ttlSeconds, refreshSeconds + 1L);

    if (!redisManager.isAvailable()) {
      logger.warning(
          "[CrossServer] suspicious-sync enabled but Redis unavailable; list stays local.");
      return;
    }

    enabled = true;
    long periodTicks = refreshSeconds * 20L;
    taskId =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                plugin, this::publishLocalSuspicious, periodTicks, periodTicks)
            .getTaskId();

    logger.info(
        "[CrossServer] Sharing suspicious players as \""
            + serverName
            + "\" (refresh "
            + refreshSeconds
            + "s, ttl "
            + ttlSeconds
            + "s).");
  }

  private void publishLocalSuspicious() {
    if (!enabled) return;
    for (MKPlayer mkPlayer : playerDataManager.getPlayers()) {
      publishPlayer(mkPlayer);
    }
  }

  private void publishPlayer(MKPlayer mkPlayer) {
    AICheck check = mkPlayer.getCheckManager().getCheck(AICheck.class);
    if (check == null || check.getBuffer() <= 0.0) return;

    Player player = mkPlayer.getPlayer();
    try {
      SuspiciousSnapshot snapshot =
          new SuspiciousSnapshot(
              serverName,
              mkPlayer.getUuid().toString(),
              player.getName(),
              check.getBuffer(),
              player.getPing(),
              System.currentTimeMillis());
      String payload = mapper.writeValueAsString(snapshot);
      redisManager.setWithTtl(
          keyPrefix + ":" + serverName + ":" + mkPlayer.getUuid(), payload, ttlSeconds);
    } catch (Exception e) {
      logger.log(Level.FINE, "[CrossServer] Failed to publish suspect " + player.getName(), e);
    }
  }

  /** Fetch suspicious players from other servers. */
  public List<SuspiciousSnapshot> fetchRemote() {
    if (!enabled) return List.of();
    return redisManager.scanValues(keyPrefix + ":*").stream()
        .map(
            raw -> {
              try {
                return mapper.readValue(raw, SuspiciousSnapshot.class);
              } catch (Exception e) {
                logger.log(Level.FINE, "[CrossServer] Bad suspect payload.", e);
                return null;
              }
            })
        .filter(s -> s != null && !s.getServer().equals(serverName))
        .toList();
  }

  public void shutdown() {
    enabled = false;
    if (taskId != -1) {
      Bukkit.getScheduler().cancelTask(taskId);
      taskId = -1;
    }
  }
}
