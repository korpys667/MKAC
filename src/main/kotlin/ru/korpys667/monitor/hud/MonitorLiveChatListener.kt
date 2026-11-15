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

import java.util.logging.Level
import java.util.logging.Logger
import ru.korpys667.api.event.AiPredictionEvent
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.monitor.core.MonitorChatStyle
import ru.korpys667.monitor.core.MonitorLabelInfo
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.core.MonitorSettingsService
import ru.korpys667.monitor.hud.output.ChatOutput
import ru.korpys667.monitor.hud.output.LiveSignal
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.scheduler.SchedulerService

@Suppress("LongParameterList")
class MonitorLiveChatListener(
  private val hudService: MonitorHudService,
  private val index: MonitorTargetIndex,
  private val chatOutput: ChatOutput,
  private val settingsService: MonitorSettingsService,
  private val frameBuilder: MonitorFrameBuilder,
  private val scheduler: SchedulerService,
  private val playerDataManager: PlayerDataManager,
  private val logger: Logger,
) {
  fun register(eventBus: MKACEventBus, pluginContext: Any) {
    eventBus.subscribe(pluginContext, AiPredictionEvent::class.java, ::onPrediction)
  }

  fun onPrediction(event: AiPredictionEvent) {
    for (viewerId in index.viewersOf(event.playerId)) {
      val session = hudService.session(viewerId)
      if (session != null && wantsLiveChat(session)) {
        scheduler.runSync(session.viewer) { deliver(session, event) }
      }
    }
  }

  private fun wantsLiveChat(session: MonitorHudSession): Boolean =
    session.outputs.any { it.kind == MonitorOutputKind.CHAT } &&
      session.chatStyle == MonitorChatStyle.LIVE

  @Suppress("TooGenericExceptionCaught")
  private fun deliver(session: MonitorHudSession, event: AiPredictionEvent) {
    try {
      val settings = settingsService.getSettings(session.viewer.uniqueId)
      val tier =
        playerDataManager.getPlayer(event.playerId)?.mitigation?.appliedTier?.name ?: "NONE"
      val frame = session.liveFrame(event, settings, frameBuilder, tier)
      if (frame != null) {
        chatOutput.deliverLive(
          session.context,
          LiveSignal(
            frame,
            event.flagged,
            MonitorLabelInfo.probabilityOfLeader(
              event.labelBuffers,
              event.labelProbabilities,
              event.probability,
            ),
            System.currentTimeMillis(),
          ),
        )
      }
    } catch (throwable: Throwable) {
      logger.log(
        Level.WARNING,
        "[Monitor] live chat delivery failed for ${session.viewer.name}",
        throwable,
      )
    }
  }
}
