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
package ru.korpys667.mkac.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.config.ConfigManager;

public class StatsReporter {
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final Gson GSON = new Gson();

  private final MKAC plugin;
  private final ConfigManager configManager;
  private final HttpClient httpClient;
  private BukkitTask heartbeatTask;

  private String apiBaseUrl;
  private String apiKey;
  private String serverIp;

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
    this.apiKey = configManager.getAiApiKey();

    if (serverUrl == null || serverUrl.isEmpty()) {
      stopHeartbeat();
      return;
    }

    try {
      URI uri = URI.create(serverUrl);
      this.apiBaseUrl = uri.getScheme() + "://" + uri.getHost();
      if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
        this.apiBaseUrl += ":" + uri.getPort();
      }

      this.serverIp = getServerIp();

      plugin.getLogger().info("АПИ: " + apiBaseUrl);
      startHeartbeat();
    } catch (Exception e) {
      plugin.getLogger().warning("Неверная ссылка: " + e.getMessage());
      stopHeartbeat();
    }
  }

  private String getServerIp() {
    try {
      String configIp = Bukkit.getServer().getIp();
      int port = Bukkit.getServer().getPort();

      if (configIp != null && !configIp.isEmpty() && !configIp.equals("0.0.0.0")) {
        return configIp + ":" + port;
      }

      InetAddress localhost = InetAddress.getLocalHost();
      String ip = localhost.getHostAddress();

      return ip + ":" + port;
    } catch (Exception e) {
      return "unknown";
    }
  }

  private void startHeartbeat() {
    stopHeartbeat();

    heartbeatTask =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                plugin,
                () -> {
                  sendHeartbeat();
                },
                100L,
                1200L);
  }

  private void stopHeartbeat() {
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
      heartbeatTask = null;
    }
  }

  private void sendHeartbeat() {
    if (!configManager.isAiEnabled()) {
      stopHeartbeat();
      return;
    }

    if (apiBaseUrl == null || apiKey == null) return;

    try {
      int onlineCount = Bukkit.getOnlinePlayers().size();

      JsonObject payload = new JsonObject();
      payload.addProperty("online_count", onlineCount);
      payload.addProperty("server_ip", serverIp);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(apiBaseUrl + "/api/server/heartbeat"))
              .header("Content-Type", "application/json")
              .header("X-API-Key", apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
              .timeout(TIMEOUT)
              .build();

      httpClient
          .sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(
              response -> {
                if (response.statusCode() == 200) {
                } else {
                  plugin.getLogger().warning("Ошибка API: " + response.statusCode());
                }
              })
          .exceptionally(
              throwable -> {
                plugin.getLogger().warning("Ошибка API: " + throwable.getMessage());
                return null;
              });

    } catch (Exception e) {
      plugin.getLogger().warning("Ошибка API: " + e.getMessage());
    }
  }

  public void reportBan(String playerName, String reason, double probability) {
    if (!configManager.isAiEnabled()) {
      return;
    }

    if (apiBaseUrl == null || apiKey == null) return;

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
                        .uri(URI.create(apiBaseUrl + "/api/server/ban"))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .timeout(TIMEOUT)
                        .build();

                httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(
                        response -> {
                          if (response.statusCode() == 200) {
                          } else {
                          }
                        });

              } catch (Exception e) {
              }
            });
  }

  public void shutdown() {
    stopHeartbeat();
  }
}
