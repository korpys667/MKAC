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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import org.bukkit.entity.Player
import org.spongepowered.configurate.CommentedConfigurationNode
import ru.korpys667.ai.label.LabelCatalog
import ru.korpys667.api.event.AiPredictionEvent
import ru.korpys667.config.ConfigView
import ru.korpys667.monitor.core.MonitorChatStyle
import ru.korpys667.monitor.core.MonitorMode
import ru.korpys667.monitor.core.MonitorNameMode
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.core.MonitorSettings
import ru.korpys667.monitor.core.MonitorSettingsService
import ru.korpys667.monitor.core.MonitorTheme
import ru.korpys667.monitor.hud.output.ChatOutput
import ru.korpys667.monitor.hud.output.LiveSignal
import ru.korpys667.scheduler.SchedulerService

class MonitorLiveChatListenerTest {
  private val targetId = UUID.randomUUID()
  private val hudService = mockk<MonitorHudService>()
  private val chatOutput = mockk<ChatOutput>(relaxed = true)
  private val settingsService = mockk<MonitorSettingsService>()
  private val scheduler = mockk<SchedulerService>()
  private val index = MonitorTargetIndex()

  private val runtimeConfig =
    MonitorHudRuntimeConfig.from(
      ConfigView(CommentedConfigurationNode.root()),
      2,
      Logger.getLogger("live-chat-test"),
    )

  private val listener =
    MonitorLiveChatListener(
      hudService,
      index,
      chatOutput,
      settingsService,
      MonitorFrameBuilder(LabelCatalog(local = { emptyMap() })),
      scheduler,
      mockk(relaxed = true),
      Logger.getLogger("live-chat-listener"),
    )

  private fun sessionFor(
    viewerId: UUID,
    kind: MonitorOutputKind,
    style: MonitorChatStyle,
  ): MonitorHudSession {
    val viewer = mockk<Player>(relaxed = true)
    every { viewer.uniqueId } returns viewerId
    every { viewer.isOnline } returns true
    val output = mockk<MonitorOutput>(relaxed = true)
    every { output.kind } returns kind
    val session =
      MonitorHudSession(
        MonitorSessionSpec(
          viewer = viewer,
          sessionId = 1L,
          chatStyle = style,
          config = runtimeConfig,
        ),
        listOf(output),
      )
    val target = mockk<Player>(relaxed = true)
    every { target.uniqueId } returns targetId
    every { target.name } returns "Steve"
    session.trackTarget(target, UnavailableTexts("no data", "no ai"))
    return session
  }

  private fun arrange(vararg sessions: Pair<UUID, MonitorHudSession?>) {
    index.set(UUID.randomUUID(), emptyList())
    sessions.forEach { (id, _) -> index.set(id, listOf(targetId)) }
    sessions.forEach { (id, session) -> every { hudService.session(id) } returns session }
    every { settingsService.getSettings(any()) } returns
      MonitorSettings(
        mode = MonitorMode.COMPACT,
        theme = MonitorTheme.CALM,
        showPing = true,
        showDmg = true,
        showTrend = true,
        showName = MonitorNameMode.AUTO,
      )
    every { scheduler.runSync(any<Player>(), any()) } answers
      {
        secondArg<Runnable>().run()
        mockk(relaxed = true)
      }
  }

  private fun event(flagged: Boolean = false) =
    AiPredictionEvent(
      playerId = targetId,
      playerName = "Steve",
      checkName = "AI",
      probability = 0.87,
      bufferBefore = 1.0,
      bufferAfter = 2.0,
      damageMultiplier = 1.0,
      prob90 = 4,
      flagged = flagged,
    )

  @Test
  fun `a chat session in live style receives the line`() {
    val viewerId = UUID.randomUUID()
    arrange(viewerId to sessionFor(viewerId, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE))

    listener.onPrediction(event())

    verify(exactly = 1) { chatOutput.deliverLive(any(), any()) }
  }

  @Test
  fun `a chat session in summary style is left to the render tick`() {
    val viewerId = UUID.randomUUID()
    arrange(viewerId to sessionFor(viewerId, MonitorOutputKind.CHAT, MonitorChatStyle.SUMMARY))

    listener.onPrediction(event())

    verify(exactly = 0) { chatOutput.deliverLive(any(), any()) }
  }

  @Test
  fun `a non-chat output never receives live lines`() {
    val viewerId = UUID.randomUUID()
    arrange(viewerId to sessionFor(viewerId, MonitorOutputKind.SIDEBAR, MonitorChatStyle.LIVE))

    listener.onPrediction(event())

    verify(exactly = 0) { chatOutput.deliverLive(any(), any()) }
  }

  @Test
  fun `a viewer whose session vanished is skipped`() {
    val viewerId = UUID.randomUUID()
    arrange(viewerId to null)

    listener.onPrediction(event())

    verify(exactly = 0) { chatOutput.deliverLive(any(), any()) }
  }

  @Test
  fun `every watching viewer gets its own line`() {
    val first = UUID.randomUUID()
    val second = UUID.randomUUID()
    arrange(
      first to sessionFor(first, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE),
      second to sessionFor(second, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE),
    )

    listener.onPrediction(event())

    verify(exactly = 2) { chatOutput.deliverLive(any(), any()) }
  }

  @Test
  fun `one throwing delivery does not stop the others`() {
    val first = UUID.randomUUID()
    val second = UUID.randomUUID()
    arrange(
      first to sessionFor(first, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE),
      second to sessionFor(second, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE),
    )
    var calls = 0
    every { chatOutput.deliverLive(any(), any()) } answers
      {
        calls++
        if (calls == 1) error("boom") else true
      }

    listener.onPrediction(event())

    assertEquals(2, calls)
  }

  @Test
  fun `the signal carries the detection's own probability and flag`() {
    val viewerId = UUID.randomUUID()
    arrange(viewerId to sessionFor(viewerId, MonitorOutputKind.CHAT, MonitorChatStyle.LIVE))
    val signals = mutableListOf<LiveSignal>()
    every { chatOutput.deliverLive(any(), capture(signals)) } returns true

    listener.onPrediction(event(flagged = true))

    assertEquals(0.87, signals.single().probability)
    assertEquals(true, signals.single().flagged)
  }
}
