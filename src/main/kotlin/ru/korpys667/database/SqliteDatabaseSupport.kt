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
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import ru.korpys667.config.ConfigManager

private const val SQLITE_BACKUP_DIRECTORY = "db-backups"
private val SQLITE_BACKUP_TIMESTAMP: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

internal fun resolveSqliteDatabaseFile(dataDirectory: Path, configManager: ConfigManager): Path {
  val fileName = configManager.config.getString("database.sqlite.file", "violations.db")
  return File(dataDirectory.toFile(), fileName).toPath()
}

internal fun createSqliteBackup(
  dataDirectory: Path,
  dataSource: HikariDataSource,
  databaseFile: Path,
): Path {
  val backupDirectory = dataDirectory.resolve(SQLITE_BACKUP_DIRECTORY)
  Files.createDirectories(backupDirectory)
  val timestamp = LocalDateTime.now().format(SQLITE_BACKUP_TIMESTAMP)
  val backupFile =
    backupDirectory.resolve("${databaseFile.fileName.toString().removeSuffix(".db")}-$timestamp.db")

  dataSource.connection.use { connection ->
    val escapedPath = backupFile.toAbsolutePath().toString().replace("'", "''")
    connection.createStatement().use { statement ->
      statement.execute("VACUUM INTO '$escapedPath'")
    }
  }

  return backupFile
}

internal fun sqliteDatabaseHasContent(databaseFile: Path): Boolean {
  return Files.exists(databaseFile) && Files.size(databaseFile) > 0L
}
