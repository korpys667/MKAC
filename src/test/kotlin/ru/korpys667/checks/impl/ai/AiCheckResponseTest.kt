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
import io.mockk.verify
import java.lang.reflect.Method
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import ru.korpys667.MKAC
import ru.korpys667.ai.AiResult
import ru.korpys667.ai.AiService
import ru.korpys667.alert.AlertManager
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.config.ConfigManager
import ru.korpys667.damage.DamageProcessor
import ru.korpys667.debug.DebugCategory
import ru.korpys667.debug.DebugManager
import ru.korpys667.mitigation.MitigationScorer
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.state.CombatState
import ru.korpys667.punishment.PunishmentManager
import ru.korpys667.region.RegionProvider
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.server.AIResponse

class AiCheckResponseTest {

  @Test
  fun `probability debug log uses fixed formatting only when enabled`() {
    val fixture = createFixture(enabledCategories = setOf(DebugCategory.AI_PROBABILITY))

    fixture.invokeOnResponse(0.42)

    verify(exactly = 1) {
      fixture.logger.info(
        "[DEBUG | AI_PROBABILITY] [TestPlayer | 1.21.4] Prob: 0.4200 | Buffer: 0.00 -> 0.00 | Damage Multiplier: 1.00"
      )
    }
  }

  @Test
  fun `flag debug string uses compact fixed formatting`() {
    val fixture = createFixture(aiFlag = 0.5, bufferMultiplier = 20.0)

    fixture.invokeOnResponse(0.95)

    verify(exactly = 1) {
      fixture.punishmentManager.handleFlag(fixture.check, any(), "prob=0.95 buffer=1.0")
    }
    assertEquals(0.0, fixture.check.buffer)
  }

  @Test
  fun `the flag detail names every label that crossed, strongest first`() {
    val debug = buildAiFlagDebug(0.97, mapOf("aim" to 51.2, "trigger" to 63.4))

    assertEquals("prob=0.97 trigger=63.4 aim=51.2", debug)
  }

  @Test
  fun `the flag detail carries every tracked label, not only the one that crossed`() {
    val debug =
      buildAiFlagDebug(
        1.0,
        crossed = mapOf("trigger" to 52.6),
        tracked = mapOf("trigger" to 25.0, "aim" to 44.9),
      )

    assertEquals(
      "prob=1.00 trigger=52.6 aim=44.9",
      debug,
      "the crossed label keeps its value from before the reset, and the quiet one is still shown",
    )
  }

  @Test
  fun `a single-headed flag still reads as one buffer`() {
    val debug = buildAiFlagDebug(0.95, mapOf(AiCheck.UNATTRIBUTED_LABEL to 51.0))

    assertEquals("prob=0.95 buffer=51.0", debug)
  }

  @Test
  fun `an unnamed buffer that crossed is shown even while a named one is still draining`() {
    val debug =
      buildAiFlagDebug(
        0.95,
        crossed = mapOf(AiCheck.UNATTRIBUTED_LABEL to 55.0),
        tracked = mapOf("aim" to 12.0),
      )

    assertEquals(
      "prob=0.95 buffer=55.0 aim=12.0",
      debug,
      "the buffer that fired is unnamed here, and hiding it left the line naming only labels " +
        "that had nothing to do with the flag",
    )
  }

  @Test
  fun `ai formatting helpers use stable decimal output`() {
    assertEquals("1.0", formatAiBuffer(1.04))
    assertEquals("0.95", formatAiProbability(0.945))
    assertEquals("0.4200", formatAiProbabilityVerbose(0.42))
  }

  private fun createFixture(
    enabledCategories: Set<DebugCategory> = emptySet(),
    aiFlag: Double = 10.0,
    bufferMultiplier: Double = 1.0,
  ): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<MKAC>(relaxed = true)
    every { plugin.logger } returns logger

    val aiService = mockk<AiService>(relaxed = true)
    every { aiService.isEnabled } returns true

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiPreWindow } returns 20
    every { configManager.aiPostWindow } returns 40
    every { configManager.aiStep } returns 1
    every { configManager.aiFlag } returns aiFlag
    every { configManager.aiResetOnFlag } returns 0.0
    every { configManager.aiBufferMultiplier } returns bufferMultiplier
    every { configManager.aiBufferDecrease } returns 0.25
    every { configManager.suspiciousAlertsBuffer } returns 25.0
    every { configManager.aiLabelMaxTracked } returns 32
    every { configManager.aiLabelSplit } returns true
    every { configManager.aiLabels } returns emptyList()
    every { configManager.effectiveLabelMode } returns null
    every { configManager.enabledDebugCategories } returns enabledCategories

    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"
    every { player.uniqueId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")

    val eventBus = mockk<MKACEventBus>(relaxed = true)
    val punishmentManager = mockk<PunishmentManager>(relaxed = true)
    val combat = CombatState()

    val user = mockk<User>(relaxed = true)
    every { user.clientVersion } returns ClientVersion.V_1_21_4

    val mkacPlayer = mockk<MKACPlayer>(relaxed = true)
    every { mkacPlayer.player } returns player
    every { mkacPlayer.uuid } returns player.uniqueId
    every { mkacPlayer.eventBus } returns eventBus
    every { mkacPlayer.punishmentManager } returns punishmentManager
    every { mkacPlayer.combat } returns combat
    every { mkacPlayer.user } returns user

    val check =
      AiCheck(
        mkacPlayer = mkacPlayer,
        plugin = plugin,
        aiService = aiService,
        configManager = configManager,
        regionProvider = mockk<RegionProvider>(relaxed = true),
        alertManager = mockk<AlertManager>(relaxed = true),
        damageProcessor = mockk<DamageProcessor>(relaxed = true),
        debugManager = DebugManager(plugin, configManager),
        scheduler = mockk<SchedulerService>(relaxed = true),
        mitigationScorer = mockk<MitigationScorer>(relaxed = true),
      )

    return Fixture(check, logger, punishmentManager)
  }

  private data class Fixture(
    val check: AiCheck,
    val logger: Logger,
    val punishmentManager: PunishmentManager,
  ) {
    fun invokeOnResponse(probability: Double) {
      onResponseMethod.invoke(
        check,
        AiResult(AIResponse(probability), """{"probability":$probability}""", null, false),
      )
    }
  }

  private companion object {
    val onResponseMethod: Method =
      AiCheck::class.java.getDeclaredMethod("onResponse", AiResult::class.java).apply {
        isAccessible = true
      }
  }
}
