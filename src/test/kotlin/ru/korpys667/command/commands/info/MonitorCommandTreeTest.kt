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

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import ru.korpys667.monitor.core.MonitorSettingsService
import ru.korpys667.monitor.hud.MonitorHudService
import ru.korpys667.monitor.hud.MonitorOutputRegistry
import ru.korpys667.sender.Sender

class MonitorCommandTreeTest {
  private class TestManager :
    CommandManager<Sender>(
      ExecutionCoordinator.simpleCoordinator(),
      CommandRegistrationHandler.nullCommandRegistrationHandler(),
    ) {
    override fun hasPermission(sender: Sender, permission: String): Boolean = true
  }

  private fun registerAll(): TestManager {
    val manager = TestManager()
    MonitorCommand(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
      .register(manager)
    MonitorSettingsCommand(
        mockk<MonitorSettingsService>(relaxed = true),
        mockk<MonitorHudService>(relaxed = true),
        mockk<MonitorOutputRegistry>(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
      )
      .register(manager)
    MonitorInfoCommand(
        mockk<MonitorSettingsService>(relaxed = true),
        mockk<MonitorHudService>(relaxed = true),
        mockk<MonitorOutputRegistry>(relaxed = true),
      )
      .register(manager)
    return manager
  }

  private fun chains(manager: TestManager): List<String> =
    manager.commands().map { command ->
      command.components().joinToString(" ") { component -> component.name() }
    }

  @Test
  fun `every monitor command registers without a duplicate chain`() {
    val registered = chains(registerAll())

    assertTrue(registered.isNotEmpty())
    assertEquals(registered.size, registered.toSet().size, "two commands claim the same chain")
  }

  @Test
  fun `the whole documented command surface is present`() {
    val registered = chains(registerAll()).toSet()

    val expected =
      listOf(
        "mkac monitor",
        "mkac monitor target",
        "mkac monitor add target",
        "mkac monitor remove target",
        "mkac monitor clear",
        "mkac monitor auto",
        "mkac monitor all",
        "mkac monitor suspicious",
        "mkac monitor manual",
        "mkac monitor stop",
        "mkac monitor list",
        "mkac monitor reset",
        "mkac monitor settings",
        "mkac monitor help",
        "mkac monitor output",
        "mkac monitor output output",
        "mkac monitor output add output",
        "mkac monitor output remove output",
        "mkac monitor set output output",
        "mkac monitor set chat style",
        "mkac monitor mode mode",
        "mkac monitor set mode mode",
        "mkac monitor theme theme",
        "mkac monitor set theme theme",
        "mkac monitor name mode",
        "mkac monitor set name mode",
        "mkac monitor ping state",
        "mkac monitor set ping state",
        "mkac monitor dmg state",
        "mkac monitor set dmg state",
        "mkac monitor trend state",
        "mkac monitor set trend state",
        "mkac prob",
        "mkac prob target",
      )

    assertTrue(
      expected.all { it in registered },
      "missing: ${expected.filterNot { it in registered }}",
    )
  }
}
