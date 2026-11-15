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
package ru.korpys667.debug

import java.util.EnumSet
import ru.korpys667.MKAC
import ru.korpys667.config.ConfigManager

class DebugManager(private val plugin: MKAC, private val configManager: ConfigManager) {
  private val enabledCategories: MutableSet<DebugCategory> =
    EnumSet.noneOf(DebugCategory::class.java)

  init {
    reload()
  }

  fun reload() {
    enabledCategories.clear()
    enabledCategories.addAll(configManager.enabledDebugCategories)
  }

  fun isEnabled(category: DebugCategory): Boolean {
    return enabledCategories.contains(category)
  }

  fun log(category: DebugCategory, message: String) {
    if (isEnabled(category)) {
      plugin.logger.info("[DEBUG | ${category.name}] $message")
    }
  }
}
