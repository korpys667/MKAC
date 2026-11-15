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
package ru.korpys667.command.commands.info

import net.kyori.adventure.text.Component
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser
import ru.korpys667.command.MKACCommand
import ru.korpys667.config.ConfigManager
import ru.korpys667.config.LocaleManager
import ru.korpys667.database.DatabaseManager
import ru.korpys667.database.Violation
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil
import ru.korpys667.utils.TimeUtil

class LogsCommand(
  private val databaseManager: DatabaseManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val scheduler: SchedulerService,
) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("logs")
        .permission("mkac.logs")
        .optional("page", IntegerParser.integerParser(1))
        .handler(this@LogsCommand::handleLogs)
    }
  }

  private fun handleLogs(context: CommandContext<Sender>) {
    val sender = context.sender()
    val page: Int = context.getOrDefault("page", 1)

    if (!configManager.config.getBoolean("history.enabled", false)) {
      MessageUtil.sendMessage(sender.nativeSender, Message.HISTORY_DISABLED)
      return
    }

    if (!databaseManager.isAvailable) {
      MessageUtil.sendMessage(sender.nativeSender, Message.STORAGE_DEGRADED)
    }

    scheduler.runAsync {
      val entriesPerPage = 10
      val violations: List<Violation> =
        databaseManager.database.getViolations(page, entriesPerPage, 0L)
      val totalLogs = databaseManager.database.getLogCount(0L)
      val maxPages =
        kotlin.math.max(1, kotlin.math.ceil(totalLogs.toDouble() / entriesPerPage).toInt())

      val header =
        MessageUtil.getMessage(
          Message.LOGS_HEADER,
          "page",
          page.toString(),
          "max_pages",
          maxPages.toString(),
        )

      val entries = violations.map { violation -> renderEntry(violation) }

      scheduler.runSync {
        sender.sendMessage(header)

        if (entries.isEmpty()) {
          MessageUtil.sendMessage(sender.nativeSender, Message.LOGS_NO_VIOLATIONS)
          return@runSync
        }

        for (entry in entries) {
          sender.sendMessage(entry)
        }
      }
    }
  }

  private fun renderEntry(violation: Violation): Component {
    val shown =
      configManager.labelCatalog.format(
        violation.labels.split(',').map(String::trim).filter(String::isNotEmpty)
      )
    return MessageUtil.getMessage(
      Message.LOGS_ENTRY,
      "server",
      violation.serverName,
      "player",
      violation.playerName,
      "check",
      violation.checkName,
      "vl",
      violation.vl.toString(),
      "verbose",
      violation.verbose,
      "timeago",
      TimeUtil.formatTimeAgo(violation.createdAt, localeManager),
      "labels",
      shown,
      "labels_line",
      MessageUtil.labelsLine(shown),
    )
  }
}
