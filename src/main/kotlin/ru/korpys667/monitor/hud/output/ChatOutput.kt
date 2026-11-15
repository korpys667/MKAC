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
package ru.korpys667.monitor.hud.output

import java.util.UUID
import org.bukkit.entity.Player
import ru.korpys667.monitor.core.MonitorChatStyle
import ru.korpys667.monitor.core.MonitorOutputKind
import ru.korpys667.monitor.hud.ChatConfig
import ru.korpys667.monitor.hud.MonitorFrame
import ru.korpys667.monitor.hud.MonitorHudRuntimeConfig
import ru.korpys667.monitor.hud.MonitorOutput
import ru.korpys667.monitor.hud.MonitorOutputCapabilities
import ru.korpys667.monitor.hud.MonitorOutputPolicy
import ru.korpys667.monitor.hud.MonitorRenderContext
import ru.korpys667.monitor.hud.MonitorRenderPayload
import ru.korpys667.monitor.hud.OutputStates
import ru.korpys667.monitor.hud.fillFrameTemplate

fun interface ChatSink {
  fun send(viewer: Player, raw: String)
}

data class LiveSignal(
  val frame: MonitorFrame,
  val flagged: Boolean,
  val probability: Double,
  val nowMillis: Long,
)

class ChatOutput(private val sink: ChatSink) : MonitorOutput {
  private val states = OutputStates<ChatState>()

  override val kind = MonitorOutputKind.CHAT

  override val capabilities =
    MonitorOutputCapabilities(
      maxTargets = CHAT_MAX_TARGETS,
      claimsClientSlot = false,
      eventDriven = true,
      requiresClear = false,
    )

  override fun isAvailable(): Boolean = true

  override fun policy(config: MonitorHudRuntimeConfig): MonitorOutputPolicy =
    MonitorOutputPolicy(keepAliveCycles = 0, minIntervalCycles = config.chat.summaryCycles)

  override fun attach(context: MonitorRenderContext): Boolean {
    states.put(context, ChatState(context.chatStyle))
    return true
  }

  override fun render(context: MonitorRenderContext, payload: MonitorRenderPayload) {
    val state = states.get(context)
    if (state == null || state.style != MonitorChatStyle.SUMMARY || !context.viewer.isOnline) {
      return
    }
    renderSummary(context, payload, state)
  }

  override fun clear(context: MonitorRenderContext) {
    val state = states.get(context) ?: return
    state.lastSummary = ""
    state.lastLineAt.clear()
  }

  override fun detach(context: MonitorRenderContext) {
    states.remove(context)
  }

  fun deliverLive(context: MonitorRenderContext, signal: LiveSignal): Boolean {
    val state = states.get(context)
    if (state == null || !shouldSendLine(state, context, signal)) {
      return false
    }
    state.lastLineAt[signal.frame.targetId] = signal.nowMillis
    val config = context.config.chat
    val template = config.templateFor(signal.flagged, signal.frame.allLabels)
    sink.send(context.viewer, fillLive(template, config, signal.frame))
    return true
  }

  private fun fillLive(template: String, config: ChatConfig, frame: MonitorFrame): String {
    val hover =
      if (frame.labels.isEmpty()) "" else " " + fillFrameTemplate(config.labelHover, frame)
    return fillFrameTemplate(template.replace(LABEL_HOVER_MARKER, hover), frame)
  }

  private fun shouldSendLine(
    state: ChatState,
    context: MonitorRenderContext,
    signal: LiveSignal,
  ): Boolean {
    if (state.style != MonitorChatStyle.LIVE || !context.viewer.isOnline) {
      return false
    }
    val config = context.config.chat
    val loudEnough =
      (signal.flagged && config.alwaysShowFlagged) || signal.probability >= config.minProbability
    val last = state.lastLineAt[signal.frame.targetId] ?: 0L
    return loudEnough && signal.nowMillis - last >= config.cooldownMillis
  }

  private fun renderSummary(
    context: MonitorRenderContext,
    payload: MonitorRenderPayload,
    state: ChatState,
  ) {
    val config = context.config.chat
    val text =
      payload.frames
        .filter { it.dataPresent && it.aiActive }
        .joinToString(separator = "\n") { fillFrameTemplate(config.summaryTemplate, it) }
    if (text.isBlank() || (config.skipUnchanged && text == state.lastSummary)) {
      return
    }
    state.lastSummary = text
    sink.send(context.viewer, text)
  }

  internal class ChatState(val style: MonitorChatStyle) {
    var lastSummary: String = ""
    val lastLineAt = HashMap<UUID, Long>()
  }
}

internal const val CHAT_MAX_TARGETS = 4
internal const val LABEL_HOVER_MARKER = "{label_hover}"
