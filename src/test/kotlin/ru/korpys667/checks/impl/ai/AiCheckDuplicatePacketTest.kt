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

import io.mockk.every
import io.mockk.mockk
import java.util.logging.Logger
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.korpys667.MKAC
import ru.korpys667.ai.AiService
import ru.korpys667.alert.AlertManager
import ru.korpys667.config.ConfigManager
import ru.korpys667.damage.DamageProcessor
import ru.korpys667.data.TickBuffer
import ru.korpys667.data.TickData
import ru.korpys667.debug.DebugManager
import ru.korpys667.mitigation.MitigationScorer
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.state.TrackingState
import ru.korpys667.region.RegionProvider
import ru.korpys667.scheduler.SchedulerService

class AiCheckDuplicatePacketTest {

  @Test
  fun `attack tick anchors and advances the inference window`() {
    val fixture = createFixture(attackThisTick = true)

    fixture.check.onDataTick(fixture.mkacPlayer)

    assertEquals(1, fixture.ticksSinceAttackMark())
  }

  @Test
  fun `tick without an attack does not anchor the window`() {
    val fixture = createFixture(attackThisTick = false)

    fixture.check.onDataTick(fixture.mkacPlayer)

    assertEquals(-1, fixture.ticksSinceAttackMark())
  }

  @Test
  fun `a data buffer swap invalidates a stale anchor`() {
    val fixture = createFixture(attackThisTick = true)

    fixture.check.onDataTick(fixture.mkacPlayer)
    assertEquals(1, fixture.ticksSinceAttackMark())

    fixture.tracking.attackThisTick = false
    fixture.tracking.windowStartThisTick = false
    every { fixture.mkacPlayer.tickBuffer } returns mockk<TickBuffer>(relaxed = true)
    fixture.check.onDataTick(fixture.mkacPlayer)

    assertEquals(-1, fixture.ticksSinceAttackMark())
  }

  private fun createFixture(attackThisTick: Boolean): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<MKAC>(relaxed = true)
    every { plugin.logger } returns logger

    val aiService = mockk<AiService>(relaxed = true)
    every { aiService.isEnabled } returns true

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiPreWindow } returns 20
    every { configManager.aiPostWindow } returns 40
    every { configManager.aiStep } returns 60
    every { configManager.aiFlag } returns 1.0
    every { configManager.aiResetOnFlag } returns 0.0
    every { configManager.aiBufferMultiplier } returns 1.0
    every { configManager.aiBufferDecrease } returns 1.0
    every { configManager.suspiciousAlertsBuffer } returns 25.0
    every { configManager.aiLabelMaxTracked } returns 32
    every { configManager.aiLabelSplit } returns true
    every { configManager.aiLabels } returns emptyList()

    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"

    val tracking =
      TrackingState().apply {
        this.attackThisTick = attackThisTick
        if (attackThisTick) raiseWindowStart(TickData.START_MELEE_PLAYER)
      }
    val tickBuffer = mockk<TickBuffer>(relaxed = true)
    val mkacPlayer = mockk<MKACPlayer>(relaxed = true)
    every { mkacPlayer.player } returns player
    every { mkacPlayer.tracking } returns tracking
    every { mkacPlayer.tickBuffer } returns tickBuffer
    every { mkacPlayer.compensatedEntities.self.riding } returns null

    val debugManager = DebugManager(plugin, configManager)
    val check =
      AiCheck(
        mkacPlayer = mkacPlayer,
        plugin = plugin,
        aiService = aiService,
        configManager = configManager,
        regionProvider = mockk<RegionProvider>(relaxed = true),
        alertManager = mockk<AlertManager>(relaxed = true),
        damageProcessor = mockk<DamageProcessor>(relaxed = true),
        debugManager = debugManager,
        scheduler = mockk<SchedulerService>(relaxed = true),
        mitigationScorer = mockk<MitigationScorer>(relaxed = true),
      )

    return Fixture(check, mkacPlayer, tracking)
  }

  private data class Fixture(
    val check: AiCheck,
    val mkacPlayer: MKACPlayer,
    val tracking: TrackingState,
  ) {
    fun ticksSinceAttackMark(): Int = check.inferenceTicks
  }
}
