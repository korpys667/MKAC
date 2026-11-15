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

import java.util.Locale
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import ru.korpys667.config.ConfigManager
import ru.korpys667.monitor.core.MonitorChatStyle
import ru.korpys667.monitor.core.MonitorMode
import ru.korpys667.monitor.core.MonitorNameMode
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.core.MonitorTheme
import ru.korpys667.monitor.hud.MonitorHudService
import ru.korpys667.monitor.hud.MonitorOutputRegistry
import ru.korpys667.monitor.hud.MonitorTargetsService
import ru.korpys667.sender.Sender

internal object MonitorSuggestions {
  val MODE: SuggestionProvider<Sender> = ofEnum(MonitorMode.entries.map { it.name })
  val THEME: SuggestionProvider<Sender> = ofEnum(MonitorTheme.entries.map { it.name })
  val NAME: SuggestionProvider<Sender> = ofEnum(MonitorNameMode.entries.map { it.name })
  val CHAT: SuggestionProvider<Sender> = ofEnum(MonitorChatStyle.entries.map { it.name })
  val TOGGLE: SuggestionProvider<Sender> =
    SuggestionProvider.suggesting(listOf("on", "off").map { Suggestion.suggestion(it) })

  fun outputs(
    hudService: MonitorHudService,
    registry: MonitorOutputRegistry,
  ): SuggestionProvider<Sender> = SuggestionProvider.blocking { context, _ ->
    val sender = context.sender()
    val config = hudService.runtimeConfig
    MonitorOutputKind.entries
      .filter { kind ->
        config.isEnabled(kind) &&
          registry.isSupported(kind) &&
          sender.nativeSender.hasPermission(kind.permission)
      }
      .map { Suggestion.suggestion(it.key) }
  }

  fun labelFocus(configManager: ConfigManager): SuggestionProvider<Sender> =
    SuggestionProvider.blocking { _, _ ->
      (listOf("auto", "off", "all") + configManager.aiLabels).map(Suggestion::suggestion)
    }

  fun watched(targets: MonitorTargetsService): SuggestionProvider<Sender> =
    SuggestionProvider.blocking { context, _ ->
      val viewer = context.sender().player
      if (viewer == null) {
        emptyList()
      } else {
        targets.names(viewer.uniqueId).map { Suggestion.suggestion(it) }
      }
    }

  private fun ofEnum(names: List<String>): SuggestionProvider<Sender> =
    SuggestionProvider.suggesting(names.map { Suggestion.suggestion(it.lowercase(Locale.ROOT)) })
}
