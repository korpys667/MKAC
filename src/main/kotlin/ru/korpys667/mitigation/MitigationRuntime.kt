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
@file:Suppress("LongParameterList", "TooManyFunctions")

package ru.korpys667.mitigation

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import ru.korpys667.MKAC
import ru.korpys667.alert.AlertManager
import ru.korpys667.alert.AlertType
import ru.korpys667.api.event.MitigationRuleEvent
import ru.korpys667.checks.impl.ai.AiCheck
import ru.korpys667.config.ConfigManager
import ru.korpys667.debug.DebugCategory
import ru.korpys667.debug.DebugManager
import ru.korpys667.platform.scheduler.TaskHandle
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

private const val PERIOD_TICKS = 20L
private const val NO_RULE = "none"
private const val ANSWER_STALE_MILLIS = 3_000L

class MitigationRuntime(
  private val plugin: MKAC,
  private val playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  private val alertManager: AlertManager,
  private val skip: MitigationSkip,
  private val engine: RuleEngine,
  private val damageProcessor: MitigationDamageProcessor,
  private val stamps: HitStamps,
  private val debugManager: DebugManager,
  private val scheduler: SchedulerService,
  private val logStore: MitigationLogStore,
  private val settings: () -> MitigationSettings,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  private var handle: TaskHandle? = null

  fun enable() {
    plugin.server.pluginManager.registerEvents(
      MitigationChannelListener(playerDataManager, stamps),
      plugin,
    )
    handle = scheduler.runTimer({ tick() }, PERIOD_TICKS, PERIOD_TICKS)
  }

  fun disable() {
    handle?.cancel()
    handle = null
    playerDataManager.getPlayers().forEach { release(it, async = false) }
    stamps.clear()
  }

  fun reload() {
    val fresh = settings().rules.associateBy { it.id }
    playerDataManager.getPlayers().forEach { mkacPlayer ->
      val state = mkacPlayer.mitigation
      val running = state.matched ?: state.applied
      if (running != null && fresh[running.id] == null) {
        release(mkacPlayer)
        return@forEach
      }
      state.matched = state.matched?.let { fresh.getValue(it.id) }
      state.applied = state.applied?.let { fresh.getValue(it.id) }
      state.spent = state.spent?.let { fresh[it.id] }
    }
  }

  fun clearFor(mkacPlayer: MKACPlayer) {
    release(mkacPlayer)
  }

  internal fun tick() {
    playerDataManager.getPlayers().forEach(::advance)
  }

  fun factsFor(mkacPlayer: MKACPlayer): RuleFacts {
    val now = clock()
    val state = mkacPlayer.mitigation
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java)
    val heard = state.lastAnswerAtMillis
    val listening = heard != 0L && now - heard <= ANSWER_STALE_MILLIS
    return RuleFacts(
      score = state.score,
      buffer = aiCheck?.buffer ?: 0.0,
      probability = if (listening) aiCheck?.lastCheatProbability ?: 0.0 else 0.0,
      answers = state.answers,
      sessions = state.history.sessions,
      days = state.history.days,
      onlineMillis = now - mkacPlayer.joinTime,
      inCombat =
        mkacPlayer.combat.ticksSinceAttack <=
          (configManager.aiPreWindow + configManager.aiPostWindow),
      probabilityHolds = if (listening) state.probabilityHolds() else emptyMap(),
      labelBuffers = aiCheck?.labelBufferSnapshot().orEmpty(),
      labelProbabilities = if (listening) aiCheck?.lastLabelProbabilities.orEmpty() else emptyMap(),
    )
  }

  private fun advance(mkacPlayer: MKACPlayer) {
    val config = settings()
    val state = mkacPlayer.mitigation
    val applying = state.appliedTier
    val wasApplied = state.applied
    val startedAt = state.appliedAtMillis
    val peak = state.peakScore

    val facts = factsFor(mkacPlayer)
    val enoughAnswers = state.answers >= config.score.minAnswers
    val reason =
      skip.skipReason(mkacPlayer) ?: (SkipReason.TOO_FEW_ANSWERS.takeIf { !enoughAnswers })

    val change = engine.evaluate(state, facts, reason)
    if (state.applied !== wasApplied && wasApplied != null && startedAt != 0L) {
      log(mkacPlayer, wasApplied, applying, startedAt, peak)
    }
    trackPeak(state, wasApplied)

    countTowardsRepeat(mkacPlayer)
    damageProcessor.refresh(mkacPlayer)

    if (change != null) announce(mkacPlayer, change)
    tellStaff(mkacPlayer, applying)
  }

  private fun countTowardsRepeat(mkacPlayer: MKACPlayer) {
    val state = mkacPlayer.mitigation
    if (state.countedThisSession || state.matched == null) return
    state.countedThisSession = true

    val today =
      Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    val seen = state.history
    val firstEver = seen.sessions == 0
    state.history =
      ScoreHistory(
        sessions = seen.sessions + 1,
        days = if (!firstEver && today == seen.lastDay) seen.days else seen.days + 1,
        lastDay = today,
      )
  }

  private fun trackPeak(state: MitigationState, wasApplied: MitigationRule?) {
    state.peakScore =
      when {
        state.applied == null -> 0.0
        state.applied !== wasApplied -> state.score
        else -> maxOf(state.peakScore, state.score)
      }
  }

  private fun log(
    mkacPlayer: MKACPlayer,
    rule: MitigationRule,
    tier: MitigationTier,
    startedAt: Long,
    peak: Double,
    async: Boolean = true,
  ) {
    if (async) {
      scheduler.runAsync { logStore.record(mkacPlayer, rule, tier, startedAt, peak) }
    } else {
      logStore.record(mkacPlayer, rule, tier, startedAt, peak)
    }
  }

  private fun release(mkacPlayer: MKACPlayer, async: Boolean = true) {
    val state = mkacPlayer.mitigation
    val running = state.applied
    if (running != null && state.appliedAtMillis != 0L) {
      log(
        mkacPlayer,
        running,
        state.appliedTier,
        state.appliedAtMillis,
        maxOf(state.peakScore, state.score),
        async,
      )
    }
    state.matched = null
    state.applied = null
    state.onsetAtMillis = clock()
    state.holdUntilMillis = clock()
    state.appliedAtMillis = 0L
    state.peakScore = 0.0
    state.spent = null
    state.activeEffects = emptyMap()
    mkacPlayer.combat.damageMultiplier = 1.0
  }

  private fun tellStaff(mkacPlayer: MKACPlayer, was: MitigationTier) {
    val state = mkacPlayer.mitigation
    val now = state.appliedTier
    if (now == was || !now.atLeast(FIRST_GAMEPLAY_TIER) || now.ordinal <= was.ordinal) return

    alertManager.send(
      MessageUtil.getMessage(
        Message.MITIGATIONS_ALERT,
        "player",
        mkacPlayer.player.name,
        "tier",
        state.tierName,
        "score",
        format(state.score),
        "active",
        state.applied?.id ?: "-",
      ),
      AlertType.MITIGATION,
    )
  }

  private fun announce(mkacPlayer: MKACPlayer, change: RuleChange) {
    val state = mkacPlayer.mitigation
    debugManager.log(
      DebugCategory.MITIGATION,
      "${mkacPlayer.player.name} ${change.from ?: "-"} -> ${change.to ?: "-"} " +
        "at ${format(state.score)}: ${change.reason}",
    )
    mkacPlayer.eventBus.post(
      MitigationRuleEvent(
        mkacPlayer.uuid,
        mkacPlayer.player.name,
        change.from ?: NO_RULE,
        change.to ?: NO_RULE,
        state.score,
        change.reason,
      )
    )
  }

  private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}
