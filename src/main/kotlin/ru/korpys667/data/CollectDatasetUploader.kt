/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
 *
 * MKAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package ru.korpys667.data

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager

class CollectDatasetUploader(
  private val plugin: MKAC,
  private val configManager: ConfigManager,
) {
  private val httpClient =
    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS)).build()

  fun upload(sessionDirectory: File) {
    val apiKey = configManager.aiApiKey.trim()
    val endpoint = DATASET_UPLOAD_URL
    if (apiKey.isEmpty()) {
      plugin.logger.warning(
        "[DataCollect] Dataset saved locally, but upload failed: AI API key is missing."
      )
    } else {
      val archive =
        try {
          createArchive(sessionDirectory)
        } catch (e: IOException) {
          logPackageFailure(sessionDirectory, e)
          null
        } catch (e: IllegalArgumentException) {
          logPackageFailure(sessionDirectory, e)
          null
        }
      if (archive != null)
        sendArchive(archive, sessionDirectory.name, endpoint, apiKey, attempt = 1)
    }
  }

  private fun logPackageFailure(sessionDirectory: File, error: Exception) {
    plugin.logger.log(
      Level.WARNING,
      "[DataCollect] Failed to package ${sessionDirectory.name} for upload",
      error,
    )
  }

  private fun createArchive(sessionDirectory: File): File {
    val files =
      sessionDirectory
        .listFiles()
        ?.filter { it.isFile && (it.name == "session.meta" || it.name.matches(WINDOW_FILE)) }
        ?.sortedBy { it.name }
        .orEmpty()
    require(files.any { it.name == "session.meta" }) { "session.meta is missing" }
    require(files.any { it.name.matches(WINDOW_FILE) }) { "No w*.csv windows found" }

    val archive = File.createTempFile("mkac-dataset-", ".zip", plugin.dataFolder)
    try {
      ZipOutputStream(Files.newOutputStream(archive.toPath())).use { zip ->
        for (file in files) {
          zip.putNextEntry(ZipEntry(file.name))
          Files.copy(file.toPath(), zip)
          zip.closeEntry()
        }
      }
    } catch (e: IOException) {
      archive.delete()
      throw e
    }
    return archive
  }

  private fun sendArchive(
    archive: File,
    sessionName: String,
    endpoint: String,
    apiKey: String,
    attempt: Int,
  ) {
    val request =
      try {
        HttpRequest.newBuilder(URI.create(endpoint))
          .timeout(Duration.ofSeconds(UPLOAD_TIMEOUT_SECONDS))
          .header("Content-Type", "application/zip")
          .header("Accept", "application/json")
          .header("User-Agent", "MKAC/${plugin.description.version}")
          .header("X-API-Key", apiKey)
          .header("X-Collect-Session", sessionName)
          .POST(HttpRequest.BodyPublishers.ofFile(archive.toPath()))
          .build()
      } catch (e: IllegalArgumentException) {
        archive.delete()
        plugin.logger.log(Level.WARNING, "[DataCollect] Invalid dataset upload endpoint", e)
        return
      } catch (e: URISyntaxException) {
        archive.delete()
        plugin.logger.log(Level.WARNING, "[DataCollect] Invalid dataset upload endpoint", e)
        return
      }

    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete {
      response,
      error ->
      if (error == null && response.statusCode() in SUCCESS_STATUS_MIN..SUCCESS_STATUS_MAX) {
        plugin.logger.info(
          "[DataCollect] Uploaded ${sessionName} to the dataset server (${response.statusCode()})."
        )
        archive.delete()
      } else if (attempt < MAX_ATTEMPTS) {
        val status = response?.statusCode()?.toString() ?: error?.cause?.message ?: error?.message
        plugin.logger.warning(
          "[DataCollect] Upload of ${sessionName} failed ($status); retry $attempt/$MAX_ATTEMPTS."
        )
        CompletableFuture.delayedExecutor(RETRY_DELAY_SECONDS, TimeUnit.SECONDS).execute {
          sendArchive(archive, sessionName, endpoint, apiKey, attempt + 1)
        }
      } else {
        val status = response?.statusCode()?.toString() ?: error?.cause?.message ?: error?.message
        plugin.logger.warning(
          "[DataCollect] Could not upload ${sessionName} after $MAX_ATTEMPTS attempts ($status). " +
            "The local dataset is preserved at ${File(plugin.dataFolder, "collect/$sessionName").path}."
        )
        archive.delete()
      }
    }
  }

  private companion object {
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val UPLOAD_TIMEOUT_SECONDS = 90L
    const val RETRY_DELAY_SECONDS = 5L
    const val MAX_ATTEMPTS = 3
    const val SUCCESS_STATUS_MIN = 200
    const val SUCCESS_STATUS_MAX = 299
    const val DATASET_UPLOAD_URL = "https://mkac.fun/api/datasets"
    val WINDOW_FILE = Regex("w\\d{1,7}\\.csv")
  }
}
