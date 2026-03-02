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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.mkac.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.config.ConfigManager;

public class StatsReporter {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final Gson GSON = new Gson();
  private static final long HEARTBEAT_DELAY_TICKS = 100L;
  private static final long HEARTBEAT_PERIOD_TICKS = 1200L;
  private final MKAC plugin;
  private final ConfigManager configManager;
  private final HttpClient httpClient;
  private final AtomicReference<BukkitTask> heartbeatTask = new AtomicReference<>();
  private final AtomicBoolean sending = new AtomicBoolean(false);
  private volatile String apiBaseUrl;
  private volatile String apiKey;

  public StatsReporter(MKAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.httpClient =
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(TIMEOUT).build();
    reload();
  }

  public void reload() {
    if (!configManager.isAiEnabled()) {
      stopHeartbeat();
      return;
    }

    String serverUrl = configManager.getAiServerUrl();
    String newApiKey = configManager.getAiApiKey();

    if (isBlank(serverUrl) || isBlank(newApiKey)) {
      stopHeartbeat();
      plugin.getLogger().warning("MKAC: API URL или ключ не настроены.");
      return;
    }

    try {
      URI uri = URI.create(serverUrl.trim());
      String baseUrl = uri.getScheme() + "://" + uri.getHost();
      int uriPort = uri.getPort();
      if (uriPort != -1 && uriPort != 80 && uriPort != 443) {
        baseUrl += ":" + uriPort;
      }

      this.apiBaseUrl = baseUrl;
      this.apiKey = newApiKey.trim();

      plugin.getLogger().info("MKAC API: " + apiBaseUrl);
      startHeartbeat();

    } catch (Exception e) {
      plugin.getLogger().warning("MKAC: Неверный API URL: " + e.getMessage());
      stopHeartbeat();
    }
  }

  public void reportBan(String playerName, String reason, double probability) {
    if (!configManager.isAiEnabled()) return;

    String url = this.apiBaseUrl;
    String key = this.apiKey;
    if (url == null || key == null) return;

    String serverIp = resolveServerIp();

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                JsonObject payload = new JsonObject();
                payload.addProperty("player_name", playerName);
                payload.addProperty("reason", reason);
                payload.addProperty("probability", probability);
                payload.addProperty("server_ip", serverIp);

                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(url + "/api/server/ban"))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", key)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .timeout(TIMEOUT)
                        .build();

                httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(
                        response -> {
                          if (response.statusCode() != 200) {
                            plugin
                                .getLogger()
                                .warning(
                                    "Ошибка репорта "
                                        + response.statusCode()
                                        + ": "
                                        + response.body());
                          }
                        })
                    .exceptionally(
                        t -> {
                          return null;
                        });

              } catch (Exception e) {
              }
            });
  }

  public void shutdown() {
    stopHeartbeat();
  }

  private void startHeartbeat() {
    stopHeartbeat();

    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                plugin, this::sendHeartbeat, HEARTBEAT_DELAY_TICKS, HEARTBEAT_PERIOD_TICKS);

    heartbeatTask.set(task);
  }

  private void stopHeartbeat() {
    BukkitTask old = heartbeatTask.getAndSet(null);
    if (old != null) {
      old.cancel();
    }
    sending.set(false);
  }

  private void sendHeartbeat() {
    if (!configManager.isAiEnabled()) {
      stopHeartbeat();
      return;
    }

    String url = this.apiBaseUrl;
    String key = this.apiKey;
    if (url == null || key == null) return;

    if (!sending.compareAndSet(false, true)) return;

    try {
      String serverIp = resolveServerIp();

      int onlineCount = Bukkit.getOnlinePlayers().size();

      JsonObject payload = new JsonObject();
      payload.addProperty("online_count", onlineCount);
      payload.addProperty("server_ip", serverIp);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url + "/api/server/heartbeat"))
              .header("Content-Type", "application/json")
              .header("X-API-Key", key)
              .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
              .timeout(TIMEOUT)
              .build();

      httpClient
          .sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(
              response -> {
                if (response.statusCode() != 200) {
                  plugin
                      .getLogger()
                      .warning(
                          "Ошибка Heartbeat " + response.statusCode() + ": " + response.body());
                }
              })
          .exceptionally(
              t -> {
                plugin.getLogger().warning("Ошибка Heartbeat: " + t.getMessage());
                return null;
              })
          .whenComplete((v, t) -> sending.set(false));

    } catch (Exception e) {
      sending.set(false);
    }
  }

  private String resolveServerIp() {
    int port = Bukkit.getServer().getPort();

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 80);
      String localIp = socket.getLocalAddress().getHostAddress();
      if (!isBlank(localIp) && !localIp.equals("0.0.0.0")) {
        return localIp + ":" + port;
      }
    } catch (Exception ignored) {
    }

    return "0.0.0.0:" + port;
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
