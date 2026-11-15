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
package ru.korpys667.monitor.hud

import ru.korpys667.config.LocaleManager
import ru.korpys667.utils.Message

internal fun rawMessageFor(localeManager: LocaleManager, key: Message, targetName: String): String =
  localeManager
    .getRawMessage(key)
    .replace(PREFIX_TAG, localeManager.getRawMessage(Message.PREFIX))
    .replace(PLAYER_TAG, targetName)

internal fun targetTexts(localeManager: LocaleManager, targetName: String): UnavailableTexts =
  UnavailableTexts(
    noData = rawMessageFor(localeManager, Message.MONITOR_NO_DATA, targetName),
    noAiCheck = rawMessageFor(localeManager, Message.MONITOR_NO_AICHECK, targetName),
  )

private const val PREFIX_TAG = "<prefix>"
private const val PLAYER_TAG = "<player>"
