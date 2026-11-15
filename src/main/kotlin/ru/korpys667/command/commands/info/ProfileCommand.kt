/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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

import java.time.Instant
import java.util.Locale
import java.util.logging.Logger
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import ru.korpys667.checks.impl.ai.AiCheck
import ru.korpys667.command.MKACCommand
import ru.korpys667.config.ConfigManager
import ru.korpys667.config.LocaleManager
import ru.korpys667.database.AiSnapshot
import ru.korpys667.database.DatabaseManager
import ru.korpys667.mitigation.ScoreMath
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.sender.Sender
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil
import ru.korpys667.utils.TimeUtil

private const val PERCENT = 100

class ProfileCommand(
  private val playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val logger: Logger,
) : MKACCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("mkac") {
      literal("profile")
        .permission("mkac.profile")
        .required("target", OfflinePlayerParser.offlinePlayerParser())
        .handler(this@ProfileCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val target: OfflinePlayer = context["target"]

    val online = target.player
    val mkacPlayer = online?.let { playerDataManager.getPlayer(it) }
    if (online == null || mkacPlayer == null) {
      offline(sender, target)
      return
    }

    show(sender, online, mkacPlayer)
  }

  private fun show(sender: CommandSender, target: Player, mkacPlayer: MKACPlayer) {
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java)

    val sessionMillis = System.currentTimeMillis() - mkacPlayer.joinTime
    var totalPlayTicks = 0L
    try {
      totalPlayTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong()
    } catch (_: IllegalArgumentException) {
      logger.fine("Failed to fetch PLAY_ONE_MINUTE for ${target.name}")
    }

    val totalPlayMillis = totalPlayTicks * 50

    MessageUtil.sendMessageList(
      sender,
      Message.PROFILE_LINES,
      "player",
      target.name,
      "ping",
      target.ping.toString(),
      "version",
      mkacPlayer.user.clientVersion.releaseName,
      "brand",
      mkacPlayer.brand,
      "session_time",
      TimeUtil.formatDuration(sessionMillis, localeManager),
      "total_playtime",
      TimeUtil.formatDuration(totalPlayMillis, localeManager),
      "ai_buffer",
      if (aiCheck != null) String.format("%.2f", aiCheck.buffer) else "N/A",
      "ai_probs_90",
      if (aiCheck != null) aiCheck.prob90.toString() else "N/A",
      "ai_labels",
      if (aiCheck != null) labelBreakdown(aiCheck.labelBufferSnapshot()) else "N/A",
      "mitigation_tier",
      mkacPlayer.mitigation.tierName,
      "mitigation_score",
      String.format(Locale.US, "%.1f", mkacPlayer.mitigation.score),
      "mitigation_active",
      mkacPlayer.mitigation.applied?.id ?: "-",
      "windows",
      mkacPlayer.mitigation.answers.toString(),
      "probabilities",
      histogram(
        mkacPlayer.mitigation.shape().let {
          AiSnapshot(0L, it.total, it.low, it.middle, it.high, 0L)
        }
      ),
    )
  }

  private fun offline(sender: CommandSender, target: OfflinePlayer) {
    if (!target.hasPlayedBefore()) {
      MessageUtil.sendMessage(sender, Message.PROFILE_NO_DATA)
      return
    }
    val uuid = target.uniqueId
    val name = target.name ?: uuid.toString()

    scheduler.runAsync {
      val database = databaseManager.database
      val snapshot = database.loadAiSnapshot(uuid)
      val buffer = database.loadAiBuffer(uuid)
      val score = database.loadMitigationScore(uuid)
      val violations = database.getLogCount(uuid)

      val lines =
        MessageUtil.getMessageList(
          Message.PROFILE_OFFLINE_LINES,
          "player",
          name,
          "seen",
          ago(snapshot?.savedAt),
          "ai_buffer",
          number(buffer?.buffer, 2),
          "buffer_seen",
          ago(buffer?.updatedAt),
          "windows",
          text(snapshot?.windows),
          "high",
          text(snapshot?.highWindows),
          "probabilities",
          histogram(snapshot),
          "mitigation_score",
          number(score?.score, 1),
          "sessions",
          score?.sessions?.toString() ?: "0",
          "days",
          score?.days?.toString() ?: "0",
          "violations",
          violations.toString(),
        )

      scheduler.runSync { lines.forEach { MessageUtil.sendMessage(sender, it) } }
    }
  }

  private fun text(value: Long?): String = value?.toString() ?: "-"

  private fun number(value: Double?, digits: Int): String =
    value?.let { String.format(Locale.US, "%.${digits}f", it) } ?: "-"

  private fun ago(millis: Long?): String =
    millis?.let { TimeUtil.formatTimeAgo(Instant.ofEpochMilli(it), localeManager) } ?: "-"

  private fun histogram(snapshot: AiSnapshot?): String {
    val total = snapshot?.let { it.low + it.middle + it.high } ?: 0L
    if (snapshot == null || total == 0L) return "-"
    return localeManager
      .getRawMessage(Message.MITIGATIONS_HISTOGRAM)
      .replace("<low-mark>", ScoreMath.LOW_TAIL_UNTIL.toString())
      .replace("<high-mark>", ScoreMath.SPIKE_FROM.toString())
      .replace("<low>", share(snapshot.low, total))
      .replace("<mid>", share(snapshot.middle, total))
      .replace("<high>", share(snapshot.high, total))
  }

  private fun share(part: Long, total: Long): String = "${part * PERCENT / total}%"

  private fun labelBreakdown(buffers: Map<String, Double>): String {
    val catalog = configManager.labelCatalog
    val visible = catalog.visible(buffers)
    if (visible.isEmpty()) return "-"
    return visible.joinToString(", ") { key ->
      "${catalog.displayName(key)} ${String.format(Locale.US, "%.2f", buffers.getValue(key))}"
    }
  }
}
