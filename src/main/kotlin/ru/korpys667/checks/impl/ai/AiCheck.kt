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

import com.github.retrooper.packetevents.PacketEvents
import java.util.concurrent.ConcurrentHashMap
import ru.korpys667.MKAC
import ru.korpys667.ai.AiResult
import ru.korpys667.ai.AiService
import ru.korpys667.ai.AiServiceException
import ru.korpys667.ai.TickSerializer
import ru.korpys667.ai.label.LabelKey
import ru.korpys667.ai.label.LabelMode
import ru.korpys667.ai.label.LabelledVerdict
import ru.korpys667.ai.label.VerdictResolver
import ru.korpys667.alert.AlertManager
import ru.korpys667.alert.AlertType
import ru.korpys667.api.event.AiPredictionEvent
import ru.korpys667.checks.AbstractCheck
import ru.korpys667.checks.CheckData
import ru.korpys667.checks.CheckFactory
import ru.korpys667.checks.Reloadable
import ru.korpys667.checks.type.TickCheck
import ru.korpys667.config.ConfigManager
import ru.korpys667.damage.DamageProcessor
import ru.korpys667.data.AttackWindowTracker
import ru.korpys667.data.TickData
import ru.korpys667.debug.DebugCategory
import ru.korpys667.debug.DebugManager
import ru.korpys667.mitigation.MitigationScorer
import ru.korpys667.player.MKACPlayer
import ru.korpys667.region.RegionCheckMode
import ru.korpys667.region.RegionProvider
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.server.AIServer
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

@CheckData(name = "AI", legacyNames = ["AI (Aim)"])
@Suppress("TooManyFunctions", "LongParameterList")
class AiCheck(
  mkacPlayer: MKACPlayer,
  private val plugin: MKAC,
  private val aiService: AiService,
  private val configManager: ConfigManager,
  private val regionProvider: RegionProvider,
  private val alertManager: AlertManager,
  private val damageProcessor: DamageProcessor,
  private val debugManager: DebugManager,
  private val scheduler: SchedulerService,
  private val mitigationScorer: MitigationScorer,
) : AbstractCheck(mkacPlayer), TickCheck, Reloadable {
  private var aiEnabled = false

  private val window = AttackWindowTracker()
  private var ticksSinceLastInference = Int.MAX_VALUE / 2

  private val labelBuffers = ConcurrentHashMap<String, ViolationBuffer>()

  private val verdicts =
    VerdictResolver(
      settings = {
        VerdictResolver.Settings(
          labels = configManager.aiLabels,
          mode = configManager.effectiveLabelMode,
          split = configManager.aiLabelSplit,
          maxTracked = configManager.aiLabelMaxTracked,
          legitClasses = configManager.aiLegitLabels,
          thresholdedLabels = configManager.aiLabelThresholds.keys,
        )
      },
      warn = { plugin.logger.warning(it) },
      severe = { plugin.logger.severe(it) },
    )

  val buffer: Double
    get() = labelBuffers.values.maxOfOrNull { it.value } ?: 0.0

  fun labelBuffer(label: String): Double = labelBuffers[label]?.value ?: 0.0

  fun labelBufferSnapshot(): Map<String, Double> =
    labelBuffers.entries.associate { entry -> entry.key to entry.value.value }

  val declaredLabels: List<String>
    get() {
      val labels = configManager.aiLabels
      val mode =
        configManager.effectiveLabelMode
          ?: if (labels.size > 1) LabelMode.MULTI_LABEL else LabelMode.SINGLE
      if (!configManager.aiLabelSplit || mode != LabelMode.MULTI_LABEL) return emptyList()
      return labels.filterNot { it in configManager.aiLegitLabels }
    }

  fun restoreBuffer(value: Double) {
    restoreLabelBuffer(UNATTRIBUTED_LABEL, value)
  }

  fun trackedLabels(): Set<String> = labelBuffers.keys.toSet()

  fun clearBuffers(): Map<String, Double> {
    val cleared = labelBufferSnapshot()
    labelBuffers.clear()
    return cleared
  }

  fun clearBuffer(label: String): Double? {
    val removed = labelBuffers.remove(label) ?: return null
    return removed.value
  }

  fun restoreLabelBuffer(label: String, value: Double) {
    bufferFor(label)?.restore(value)
  }

  private fun bufferFor(label: String): ViolationBuffer? {
    val existing = labelBuffers[label]
    return when {
      existing != null -> existing
      labelBuffers.size >= configManager.aiLabelMaxTracked.coerceAtLeast(1) && !evictWeakest() ->
        null
      else -> labelBuffers.getOrPut(label) { ViolationBuffer() }
    }
  }

  private fun evictWeakest(): Boolean {
    val weakest = labelBuffers.entries.minByOrNull { it.value.value }
    if (weakest == null || weakest.value.value >= flag) {
      if (weakest != null && evictionWarned.compareAndSet(false, true)) {
        plugin.logger.warning(
          "[AiCheck] Tracking ${labelBuffers.size} labels and every one is near its flag; " +
            "refusing new ones. Check what the inference server is sending."
        )
      }
      return false
    }
    labelBuffers.remove(weakest.key)
    return true
  }

  fun feedBuffers(verdict: LabelledVerdict): Map<String, Double> {
    val settings = bufferSettings()
    val crossed = mutableMapOf<String, Double>()
    for ((label, probability) in verdict.values) {
      val labelBuffer = bufferFor(label) ?: continue
      labelBuffer.feed(probability, bufferSettings(label))
      if (labelBuffer.value > flag) {
        crossed[label] = labelBuffer.value
        labelBuffer.consumeFlag(bufferResetOnFlag)
      }
    }
    if (verdict.attributed) retireAbsent(verdict, settings.decrease)
    return crossed
  }

  private fun retireAbsent(verdict: LabelledVerdict, decrease: Double) {
    val declared = configManager.aiLabels.toSet()
    val iterator = labelBuffers.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (entry.key in verdict.values) continue
      val undeclared =
        declared.isNotEmpty() && entry.key !in declared && !LabelKey.isReserved(entry.key)
      if (undeclared) {
        iterator.remove()
        debugManager.log(
          DebugCategory.AI_PERSISTENT_BUFFER,
          "${mkacPlayer.player.name} dropped ${entry.key}=${"%.1f".format(entry.value.value)}, " +
            "the model no longer has that label",
        )
      } else if (entry.value.decay(decrease) <= 0.0) {
        iterator.remove()
      }
    }
  }

  private fun leadingLabelName(): String {
    val catalog = configManager.labelCatalog
    return catalog.leading(labelBufferSnapshot())?.let(catalog::displayName) ?: ""
  }

  private fun bufferSettings(label: String? = null): ViolationBuffer.Settings {
    val own = label?.let { configManager.aiLabelThresholds[it] }
    return ViolationBuffer.Settings(
      own?.cheat ?: CHEAT_PROBABILITY,
      own?.legit ?: LEGIT_PROBABILITY,
      bufferMultiplier,
      bufferDecrease,
    )
  }

  @Volatile
  var lastProbability: Double = 0.0
    private set

  @Volatile
  var lastCheatProbability: Double = 0.0
    private set

  @Volatile
  var lastLabelProbabilities: Map<String, Double> = emptyMap()
    private set

  @Volatile var prob90: Int = 0

  val trail = ProbabilityTrail()

  val inferenceTicks: Int
    get() = window.ticksSinceAttack

  val inferencePostWindow: Int
    get() = configManager.aiPostWindow

  val inferenceProgress: IntArray?
    get() {
      val anchored = window.ticksSinceAttack >= 0
      val stepThrottles = configManager.aiStep > configManager.aiPostWindow
      val leftWindow = if (anchored) configManager.aiPostWindow - window.ticksSinceAttack else -1
      val leftStep = configManager.aiStep - ticksSinceLastInference
      return when {
        stepThrottles && leftStep > leftWindow && leftStep > 0 ->
          intArrayOf(ticksSinceLastInference, configManager.aiStep)
        anchored -> intArrayOf(window.ticksSinceAttack, configManager.aiPostWindow)
        else -> null
      }
    }

  private var flag = 0.0
  private var bufferResetOnFlag = 0.0
  private var bufferMultiplier = 0.0
  private var bufferDecrease = 0.0
  private var suspiciousAlertBuffer = 0.0
  private val evictionWarned = java.util.concurrent.atomic.AtomicBoolean(false)

  init {
    reload()
  }

  interface Factory : CheckFactory {
    override fun create(player: MKACPlayer): AiCheck
  }

  override fun reload() {
    aiEnabled = aiService.isEnabled
    flag = configManager.aiFlag
    bufferResetOnFlag = configManager.aiResetOnFlag
    bufferMultiplier = configManager.aiBufferMultiplier
    bufferDecrease = configManager.aiBufferDecrease
    suspiciousAlertBuffer = configManager.suspiciousAlertsBuffer
  }

  override fun onDataTick(player: MKACPlayer) {
    if (!aiEnabled) return

    if (mkacPlayer.compensatedEntities.self.riding != null) {
      mitigationScorer.freeze(mkacPlayer)
      return
    }
    mitigationScorer.thaw(mkacPlayer)

    if (ticksSinceLastInference < Int.MAX_VALUE) ticksSinceLastInference++

    window.onTick(
      player.tickBuffer,
      player.tracking.windowStartThisTick,
      player.tracking.windowStartKind,
      configManager.aiPostWindow,
    ) { ticks, attackIndex, kind ->
      if (kind == TickData.START_MELEE_PLAYER && ticksSinceLastInference >= configManager.aiStep) {
        sendInference(ticks, attackIndex)
        ticksSinceLastInference = 0
      }
    }
  }

  private fun inDisabledRegion(): Boolean =
    configManager.isAiWorldGuardEnabled() &&
      regionProvider.isPlayerInDisabledRegion(mkacPlayer.player)

  private fun sendInference(ticksSinceAttack: Int, attackIndex: Int) {
    val window =
      mkacPlayer.tickBuffer.extractWindow(
        configManager.aiPreWindow,
        configManager.aiPostWindow,
        ticksSinceAttack,
        attackIndex,
      ) ?: return

    if (configManager.regionCheckMode == RegionCheckMode.SKIP_DETECTION && inDisabledRegion()) {
      debugManager.log(
        DebugCategory.WORLDGUARD,
        "Player ${mkacPlayer.player.name} is in a disabled region. Skipping AI check.",
      )
      return
    }

    val bytes =
      TickSerializer.serialize(
        window,
        mkacPlayer.user.clientVersion.protocolVersion,
        PacketEvents.getAPI().serverManager.version.protocolVersion,
        configManager.aiColumnMask,
      )
    val player = mkacPlayer.player
    val playerName = player.name

    scheduler.runAsync {
      try {
        aiService.request(bytes).whenCompleteAsync({ parsed, error ->
          if (error != null) onError(error) else onResponse(parsed)
        }) { runnable ->
          scheduler.runSync(player, runnable)
        }
      } catch (e: Exception) {
        plugin.logger.warning("[AiCheck] Failed to send data for $playerName: ${e.message}")
      }
    }
  }

  private fun onResponse(parsed: AiResult) {
    val mkacPlayer = mkacPlayer
    if (parsed.disabled) {
      lastProbability = 0.0
      lastCheatProbability = 0.0
      lastLabelProbabilities = emptyMap()
      damageProcessor.reset(mkacPlayer)
      return
    }

    if (parsed.hasParseError()) {
      plugin.logger.warning(
        "[AiCheck] Error parsing API response: ${parsed.parseError?.message}. Response Body: ${parsed.raw}"
      )
      lastProbability = 0.0
      lastCheatProbability = 0.0
      lastLabelProbabilities = emptyMap()
      damageProcessor.reset(mkacPlayer)
      return
    }

    val apiResponse = parsed.response

    if (apiResponse == null) {
      plugin.logger.warning(
        "[AiCheck] API response is missing probability. Response: ${parsed.raw}"
      )
      lastProbability = 0.0
      lastCheatProbability = 0.0
      lastLabelProbabilities = emptyMap()
      damageProcessor.reset(mkacPlayer)
      return
    }

    applyServerCorrections(apiResponse)

    val probability = apiResponse.probability
    lastProbability = probability
    trail.record(probability)

    val verdict =
      verdicts.resolve(apiResponse.probabilities, probability, apiResponse.namedProbabilities)
    val cheatProbability = verdict.values.values.maxOrNull() ?: 0.0
    if (verdict.attributed) {
      lastCheatProbability = cheatProbability
      lastLabelProbabilities = verdict.values
      mitigationScorer.record(mkacPlayer, cheatProbability, verdict.values)
      damageProcessor.applyProbability(mkacPlayer, cheatProbability)
      if (cheatProbability > CHEAT_PROBABILITY) {
        prob90++
      }
    }

    val oldBuffer = buffer
    val crossed = feedBuffers(verdict)

    if (buffer > suspiciousAlertBuffer && oldBuffer <= suspiciousAlertBuffer) {
      alertManager.send(
        MessageUtil.getMessage(
          Message.SUSPICIOUS_ALERT_TRIGGERED,
          "player",
          mkacPlayer.player.name,
          "buffer",
          formatAiBuffer(buffer),
          "label",
          leadingLabelName(),
        ),
        AlertType.SUSPICIOUS,
      )
    }

    if (debugManager.isEnabled(DebugCategory.AI_PROBABILITY)) {
      debugManager.log(
        DebugCategory.AI_PROBABILITY,
        buildAiProbabilityDebugMessage(
          playerName = "${mkacPlayer.player.name} | ${mkacPlayer.user.clientVersion.releaseName}",
          probability = probability,
          oldBuffer = oldBuffer,
          newBuffer = buffer,
          damageMultiplier = mkacPlayer.combat.damageMultiplier,
        ),
      )
    }

    var flagged = false
    if (crossed.isNotEmpty()) {
      if (configManager.regionCheckMode == RegionCheckMode.SKIP_PUNISHMENT && inDisabledRegion()) {
        debugManager.log(
          DebugCategory.WORLDGUARD,
          "Player ${mkacPlayer.player.name} is in a disabled region. Skipping punishment.",
        )
      } else {
        flagged = true
        flag(buildAiFlagDebug(probability, crossed, labelBufferSnapshot()), crossed.keys)
      }
    }

    mkacPlayer.eventBus.post(
      AiPredictionEvent(
        mkacPlayer.uuid,
        mkacPlayer.player.name,
        checkName,
        probability,
        oldBuffer,
        buffer,
        mkacPlayer.combat.damageMultiplier,
        prob90,
        flagged,
        labelBufferSnapshot(),
        verdict.values,
      )
    )
  }

  private fun applyServerCorrections(response: ru.korpys667.server.AIResponse) {
    val carriesCorrections =
      response.expectedColumns != null ||
        response.labels != null ||
        response.labelTitles != null ||
        response.legitLabels != null ||
        response.labelMode != null ||
        response.modelTitle != null ||
        response.labelThresholds != null
    if (!carriesCorrections) return
    configManager.updateAiParams(
      null,
      null,
      null,
      columns = response.expectedColumns,
      labels = response.labels,
      labelNames = response.labelTitles,
      legitLabels = response.legitLabels,
      modelTitle = response.modelTitle,
      labelMode = response.labelMode,
      labelThresholds = response.labelThresholds,
    )
  }

  @Suppress("ReturnCount")
  internal fun onError(error: Throwable): Void? {
    lastProbability = 0.0
    lastCheatProbability = 0.0
    lastLabelProbabilities = emptyMap()
    val mkacPlayer = mkacPlayer
    damageProcessor.reset(mkacPlayer)

    val cause = (error as? java.util.concurrent.CompletionException)?.cause ?: error

    val ex = cause as? AiServiceException
    if (ex != null && ex.hasNewParams) {
      val moved =
        configManager.updateAiParams(
          ex.newPreWindow,
          ex.newPostWindow,
          ex.newStep,
          model = ex.newModel,
          columns = ex.newColumns,
          labels = ex.newLabels,
          labelNames = ex.newLabelNames,
          legitLabels = ex.newLegitLabels,
          labelMode = ex.newLabelMode,
          labelThresholds = ex.newLabelThresholds,
          modelTitle = ex.newModelTitle,
        )
      if (moved) {
        stuckReconfigures.set(0)
        stuckReported.set(false)
      } else {
        noteStuckReconfigure(ex)
      }
      return null
    }

    if (cause is AIServer.RequestException) {
      if (cause.code == AIServer.ResponseCode.WAITING) {
        return null
      }

      val reason = cause.serverMessage ?: cause.message
      val logMessage =
        "[AiCheck] API Error ${cause.serverCode ?: cause.code} for player " +
          "${mkacPlayer.player.name}: $reason"

      val transientCategory = transientCategoryFor(cause.code)
      if (transientCategory != null) {
        debugManager.log(transientCategory, logMessage)
      } else {
        plugin.logger.warning(logMessage)
      }
    } else {
      plugin.logger.warning(
        "[AiCheck] Unknown API Error for ${mkacPlayer.player.name}: ${cause.message}"
      )
    }
    return null
  }

  private fun noteStuckReconfigure(ex: AiServiceException) {
    if (stuckReconfigures.incrementAndGet() < STUCK_RECONFIGURES) return
    if (!stuckReported.compareAndSet(false, true)) return
    plugin.logger.severe(
      "[AiCheck] Stuck after $STUCK_RECONFIGURES rejected windows, nothing is being scored. " +
        "Asked for ${describeAsk(ex)}; holding ${configManager.describeModelConfig()}. " +
        "Labels are folded to lowercase with underscores here, and a threshold outside 0..1 or " +
        "a cheat mark below its legit mark is dropped, so either can differ silently."
    )
  }

  private fun describeAsk(ex: AiServiceException): String = buildString {
    append("model=").append(ex.newModel ?: "-")
    append(" window=").append(ex.newPreWindow ?: "-").append('/').append(ex.newPostWindow ?: "-")
    append(" step=").append(ex.newStep ?: "-")
    append(" labels=").append(ex.newLabels?.joinToString(",") ?: "-")
    append(" legit=").append(ex.newLegitLabels?.joinToString(",") ?: "-")
    append(" mode=").append(ex.newLabelMode ?: "-")
    append(" thresholds=").append(ex.newLabelThresholds?.keys?.sorted()?.joinToString(",") ?: "-")
  }

  private fun transientCategoryFor(code: AIServer.ResponseCode): DebugCategory? =
    when (code) {
      AIServer.ResponseCode.TIMEOUT -> DebugCategory.AI_API_TIMEOUT
      AIServer.ResponseCode.NETWORK_ERROR -> DebugCategory.AI_API_NETWORK
      AIServer.ResponseCode.RATE_LIMITED -> DebugCategory.AI_API_RATE_LIMITED
      AIServer.ResponseCode.SERVICE_UNAVAILABLE -> DebugCategory.AI_API_SERVICE_UNAVAILABLE
      else -> null
    }

  companion object {
    const val UNATTRIBUTED_LABEL = LabelKey.UNATTRIBUTED

    private const val CHEAT_PROBABILITY = 0.90
    private const val LEGIT_PROBABILITY = 0.10

    private const val STUCK_RECONFIGURES = 3

    private val stuckReconfigures = java.util.concurrent.atomic.AtomicInteger(0)
    private val stuckReported = java.util.concurrent.atomic.AtomicBoolean(false)

    fun forgetStuckReconfigures() {
      stuckReconfigures.set(0)
      stuckReported.set(false)
    }
  }
}
