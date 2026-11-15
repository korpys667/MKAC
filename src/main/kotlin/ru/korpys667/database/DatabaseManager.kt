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

import com.zaxxer.hikari.HikariDataSource
import java.util.logging.Level
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager

class DatabaseManager(plugin: MKAC, configManager: ConfigManager) {
  val database: ViolationDatabase
  private val dataSource: HikariDataSource?
  private val health = DatabaseHealth()

  init {
    val rawType = configManager.config.getString("database.type", "sqlite")
    val databaseType = DatabaseType.fromConfig(rawType)
    val environment =
      DatabaseEnvironment(
        dataDirectory = plugin.dataFolder.toPath(),
        logger = plugin.logger,
        classLoader = plugin.javaClass.classLoader,
      )
    val dataSourceFactory = DatabaseDataSourceFactory(environment, configManager)
    val fallbackDatabase = InMemoryViolationDatabase(configManager)

    if (DatabaseType.entries.none { it.name.equals(rawType, ignoreCase = true) }) {
      environment.logger.warning(
        "Unknown database type $rawType, defaulting to sqlite. Supported types: sqlite, mysql, mariadb."
      )
    }

    val dataSourceResult = runCatching {
      dataSourceFactory.create(databaseType)
    }

    if (dataSourceResult.isSuccess) {
      val resolvedDataSource = dataSourceResult.getOrThrow()
      dataSource = resolvedDataSource
      val persistentDatabase =
        SqlViolationDatabase(
          configManager = configManager,
          database = org.jetbrains.exposed.v1.jdbc.Database.connect(resolvedDataSource),
        )
      database =
        ResilientViolationDatabase(
          primary = persistentDatabase,
          fallback = fallbackDatabase,
          health = health,
          logger = plugin.logger,
        )
      health.markPersistent()
    } else {
      val failure = dataSourceResult.exceptionOrNull()!!
      environment.logger.log(
        Level.WARNING,
        buildString {
          append("Persistent database storage is unavailable")
          append(". MKAC will continue in degraded mode using in-memory storage")
          append(
            ". Data will work for the current runtime, but it will not persist across restart."
          )
        },
        failure,
      )
      dataSource = null
      database = fallbackDatabase
      health.markDegraded(failure)
    }
  }

  val isAvailable: Boolean
    get() = health.isPersistentAvailable()

  val failureCause: Throwable?
    get() = health.failureCause

  fun shutdown() {
    val dataSource = dataSource ?: return
    if (!dataSource.isClosed) {
      dataSource.close()
    }
  }
}
