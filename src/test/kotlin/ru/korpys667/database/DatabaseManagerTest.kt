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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.database

import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager
import ru.korpys667.monitor.core.MonitorMode
import ru.korpys667.monitor.core.MonitorNameMode
import ru.korpys667.monitor.core.MonitorSettings
import ru.korpys667.monitor.core.MonitorTheme

class DatabaseManagerTest {

  @Test
  fun `sqlite data written through database manager survives restart`() {
    val runtime = createRuntime("sqlite-restart")
    val playerId = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
    val settings =
      MonitorSettings(
        mode = MonitorMode.COMPACT,
        theme = MonitorTheme.CALM,
        showPing = true,
        showDmg = false,
        showTrend = true,
        showCollect = true,
        showInference = true,
        showName = MonitorNameMode.AUTO,
      )

    createManager(runtime).use { manager ->
      assertTrue(manager.isAvailable)
      assertEquals(1, manager.database.incrementViolationLevel(playerId, "default"))
      assertEquals(2, manager.database.incrementViolationLevel(playerId, "default"))
      manager.database.saveMonitorSettings(playerId, settings)
    }

    createManager(runtime).use { manager ->
      assertTrue(manager.isAvailable)
      assertEquals(2, manager.database.getViolationLevel(playerId, "default"))
      assertEquals(settings, manager.database.loadMonitorSettings(playerId))
    }
  }

  private fun createManager(runtime: TestRuntime): DatabaseManager {
    return DatabaseManager(runtime.plugin, runtime.configManager)
  }

  private fun createRuntime(name: String): TestRuntime {
    val dataDirectory = Files.createTempDirectory("mkac-db-manager-$name-")
    copyResourceTo(dataDirectory, "config.yml")
    copyResourceTo(dataDirectory, "punishments.yml")
    copyResourceTo(dataDirectory, "monitor.yml")

    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<MKAC>(relaxed = true)
    every { plugin.dataFolder } returns dataDirectory.toFile()
    every { plugin.logger } returns logger

    return TestRuntime(
      dataDirectory = dataDirectory,
      databaseFile = dataDirectory.resolve("violations.db"),
      jdbcUrl = "jdbc:sqlite:${dataDirectory.resolve("violations.db").absolutePathString()}",
      plugin = plugin,
      logger = logger,
      configManager = ConfigManager(plugin),
    )
  }

  private fun copyResourceTo(directory: Path, resourceName: String) {
    javaClass.classLoader.getResourceAsStream(resourceName).use { stream ->
      checkNotNull(stream) { "Missing test resource $resourceName" }
      Files.newOutputStream(directory.resolve(resourceName)).use { output -> stream.copyTo(output) }
    }
  }

  private data class TestRuntime(
    val dataDirectory: Path,
    val databaseFile: Path,
    val jdbcUrl: String,
    val plugin: MKAC,
    val logger: Logger,
    val configManager: ConfigManager,
  )

  private fun DatabaseManager.use(block: (DatabaseManager) -> Unit) {
    try {
      block(this)
    } finally {
      shutdown()
    }
  }
}
