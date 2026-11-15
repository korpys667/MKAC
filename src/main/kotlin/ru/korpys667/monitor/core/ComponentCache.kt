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
package ru.korpys667.monitor.core

import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import ru.korpys667.utils.MessageUtil

class ComponentCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {
  private val cache = ConcurrentHashMap<String, Component>()

  fun component(raw: String): Component {
    val cached = cache[raw]
    if (cached != null) {
      return cached
    }

    if (cache.size >= maxSize) {
      cache.clear()
    }

    val parsed = MessageUtil.deserializeRaw(raw)
    val existing = cache.putIfAbsent(raw, parsed)
    return existing ?: parsed
  }

  private companion object {
    const val DEFAULT_MAX_SIZE = 256
  }
}
