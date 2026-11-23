/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.telemetry

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager
import ru.korpys667.scheduler.SchedulerService

/**
 * Reports every alert the plugin actually sends to the panel.
 *
 * One alert = one detection for this server: the panel increments total_detections, the hourly
 * activity bucket and activity_log. Reporting is fire-and-forget and never blocks the server
 * thread.
 */
class DetectionReporter(
  private val plugin: MKAC,
  private val configManager: ConfigManager,
  private val scheduler: SchedulerService,
) {
  private data class Detection(
    val playerName: String,
    val checkName: String,
    val violationLevel: Int,
    val labels: List<String>,
  )

  private val mapper = ObjectMapper()
  private val client: HttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()

  fun report(playerName: String, checkName: String, violationLevel: Int, labels: List<String>) {
    val key = configManager.aiApiKey
    if (!configManager.isTelemetryEnabled() || !keyValid(key)) return
    val url = alertUrl() ?: return
    val detection = Detection(playerName, checkName, violationLevel, labels)
    scheduler.runAsync { send(url, key, detection) }
  }

  private fun send(url: String, key: String, detection: Detection) {
    try {
      val body =
        buildMap<String, Any?> {
          put("player_name", detection.playerName)
          put("check", detection.checkName)
          put("violation_level", detection.violationLevel)
          put("labels", detection.labels)
          put("plugin_version", plugin.description.version)
        }
      val request =
        HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("X-API-Key", key)
          .header("User-Agent", "MKAC/" + plugin.description.version)
          .timeout(REQUEST_TIMEOUT)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build()
      val response = client.send(request, HttpResponse.BodyHandlers.discarding())
      if (response.statusCode() !in HTTP_OK_MIN..HTTP_OK_MAX) {
        plugin.logger.fine("[Detect] report failed: HTTP ${response.statusCode()}")
      }
    } catch (e: IOException) {
      logFailure(e)
    } catch (e: JsonProcessingException) {
      logFailure(e)
    } catch (e: IllegalArgumentException) {
      logFailure(e)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      logFailure(e)
    }
  }

  private fun logFailure(error: Exception) {
    plugin.logger.fine("[Detect] report failed: ${error.message}")
  }

  private fun alertUrl(): String? {
    val base = configManager.aiServerUrl.trim().trimEnd('/')
    if (base.isBlank()) return null
    val root = if (base.endsWith("/api")) base else "$base/api"
    return "$root/server/alert"
  }

  private fun keyValid(key: String): Boolean = key.isNotBlank() && key != PLACEHOLDER_KEY

  private companion object {
    const val PLACEHOLDER_KEY = "API-KEY"
    const val HTTP_OK_MIN = 200
    const val HTTP_OK_MAX = 299
    val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
    val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(8)
  }
}
