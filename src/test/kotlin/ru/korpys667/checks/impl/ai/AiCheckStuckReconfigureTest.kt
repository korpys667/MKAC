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
package ru.korpys667.checks.impl.ai

import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.User
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.korpys667.MKAC
import ru.korpys667.ai.AiService
import ru.korpys667.ai.AiServiceException
import ru.korpys667.alert.AlertManager
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.config.ConfigManager
import ru.korpys667.damage.DamageProcessor
import ru.korpys667.debug.DebugManager
import ru.korpys667.mitigation.MitigationScorer
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.state.CombatState
import ru.korpys667.punishment.PunishmentManager
import ru.korpys667.region.RegionProvider
import ru.korpys667.scheduler.SchedulerService

class AiCheckStuckReconfigureTest {

  private val severe = mutableListOf<String>()

  @BeforeEach fun forgetPreviousRounds() = AiCheck.forgetStuckReconfigures()

  @Test
  fun `a negotiation that never moves is called out, and only once`() {
    val check = createCheck(moves = false)

    repeat(50) { check.onError(reconfigure()) }

    assertEquals(1, severe.size, "one stuck server must not write fifty lines a second")
    assertTrue(severe.first().contains("labels=Aim Assist"), "the raw ask has to be in the line")
    assertTrue(
      severe.first().contains("lowercase with underscores"),
      "the line has to name the likeliest cause, or it is just a complaint",
    )
  }

  @Test
  fun `a negotiation that does move says nothing at all`() {
    val check = createCheck(moves = true)

    repeat(50) { check.onError(reconfigure()) }

    assertEquals(emptyList(), severe, "reconfiguring is normal; only failing to reconfigure is not")
  }

  @Test
  fun `two windows that go nowhere are not yet a stuck server`() {
    val check = createCheck(moves = false)

    repeat(2) { check.onError(reconfigure()) }

    assertEquals(emptyList(), severe, "a racing pair of in-flight windows is not a loop")
  }

  private fun reconfigure() =
    java.util.concurrent.CompletionException(
      AiServiceException(
        RuntimeException("reconfigure"),
        newPreWindow = 32,
        newPostWindow = 32,
        newStep = 32,
        newLabels = listOf("Aim Assist"),
        newLegitLabels = listOf("clean"),
        newLabelMode = "multilabel",
      )
    )

  private fun createCheck(moves: Boolean): AiCheck {
    val logger =
      mockk<Logger>(relaxed = true) {
        every { severe(any<String>()) } answers { severe += firstArg<String>() }
      }
    val plugin = mockk<MKAC>(relaxed = true)
    every { plugin.logger } returns logger

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiLabelMaxTracked } returns 32
    every { configManager.aiLabels } returns emptyList()
    every { configManager.enabledDebugCategories } returns emptySet()
    every { configManager.describeModelConfig() } returns "labels=aim_assist mode=multilabel"
    every {
      configManager.updateAiParams(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns moves

    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"
    every { player.uniqueId } returns UUID.fromString("00000000-0000-0000-0000-000000000002")

    val user = mockk<User>(relaxed = true)
    every { user.clientVersion } returns ClientVersion.V_1_21_4

    val mkacPlayer = mockk<MKACPlayer>(relaxed = true)
    every { mkacPlayer.player } returns player
    every { mkacPlayer.uuid } returns player.uniqueId
    every { mkacPlayer.eventBus } returns mockk<MKACEventBus>(relaxed = true)
    every { mkacPlayer.punishmentManager } returns mockk<PunishmentManager>(relaxed = true)
    every { mkacPlayer.combat } returns CombatState()
    every { mkacPlayer.user } returns user

    return AiCheck(
      mkacPlayer = mkacPlayer,
      plugin = plugin,
      aiService = mockk<AiService>(relaxed = true),
      configManager = configManager,
      regionProvider = mockk<RegionProvider>(relaxed = true),
      alertManager = mockk<AlertManager>(relaxed = true),
      damageProcessor = mockk<DamageProcessor>(relaxed = true),
      debugManager = DebugManager(plugin, configManager),
      scheduler = mockk<SchedulerService>(relaxed = true),
      mitigationScorer = mockk<MitigationScorer>(relaxed = true),
    )
  }
}
