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
package ru.korpys667.integration

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException
import java.util.logging.Level
import java.util.logging.Logger
import org.bukkit.Bukkit

object MKACFlags {
  const val CHECKS_FLAG_NAME = "mkac-checks"

  @Volatile
  var checks: StateFlag? = null
    private set

  fun register(logger: Logger) {
    if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
      return
    }

    val registry =
      try {
        WorldGuard.getInstance().flagRegistry
      } catch (failure: NoClassDefFoundError) {
        logger.log(
          Level.FINE,
          "WorldGuard classes unavailable, skipping flag registration",
          failure,
        )
        return
      }

    try {
      val flag = StateFlag(CHECKS_FLAG_NAME, false)
      registry.register(flag)
      checks = flag
    } catch (conflict: FlagConflictException) {
      val existing = registry.get(CHECKS_FLAG_NAME)
      if (existing is StateFlag) {
        checks = existing
      } else {
        logger.log(
          Level.WARNING,
          "Flag $CHECKS_FLAG_NAME already registered with incompatible type",
          conflict,
        )
      }
    } catch (illegal: IllegalStateException) {
      logger.log(Level.WARNING, "Failed to register WorldGuard flag $CHECKS_FLAG_NAME", illegal)
    }
  }
}
