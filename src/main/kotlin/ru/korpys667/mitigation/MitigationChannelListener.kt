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
@file:Suppress("ReturnCount")

package ru.korpys667.mitigation

import kotlin.random.Random
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import ru.korpys667.api.event.MitigationEvent
import ru.korpys667.player.MKACPlayer
import ru.korpys667.player.PlayerDataManager

private const val SURVIVES_BY = 0.5

@Suppress("TooManyFunctions")
class MitigationChannelListener(
  private val playerDataManager: PlayerDataManager,
  private val stamps: HitStamps,
  private val random: Random = Random.Default,
) : Listener {

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onDamage(event: EntityDamageByEntityEvent) {
    val victim = event.entity as? Player ?: return
    val direct = event.damager
    val attacker = causingPlayer(direct)

    if (attacker == null) {
      scaleStamped(event, victim, direct?.uniqueId)
      boostIncoming(event, victim)
      return
    }
    if (attacker.uniqueId == victim.uniqueId) return
    boostIncoming(event, victim)

    val mkacPlayer = playerDataManager.getPlayer(attacker) ?: return

    if (direct !is Projectile && cancelled(mkacPlayer)) {
      event.isCancelled = true
      return
    }

    val channel =
      if (direct is Projectile) MitigationSettings.PROJECTILE else MitigationSettings.MELEE
    scale(event, mkacPlayer, channel, multiplierFrom(mkacPlayer, direct, channel))
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onLaunch(event: ProjectileLaunchEvent) {
    val projectile = event.entity
    val shooter = projectile.shooter as? Player ?: return
    val mkacPlayer = playerDataManager.getPlayer(shooter) ?: return
    stamps.remember(
      projectile.uniqueId,
      mkacPlayer.uuid,
      multiplier(mkacPlayer, MitigationSettings.PROJECTILE),
    )
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onCrystalHit(event: EntityDamageByEntityEvent) {
    val crystal = event.entity as? EnderCrystal ?: return
    val attacker = causingPlayer(event.damager) ?: return
    val mkacPlayer = playerDataManager.getPlayer(attacker) ?: return
    stamps.remember(
      crystal.uniqueId,
      mkacPlayer.uuid,
      multiplier(mkacPlayer, MitigationSettings.CRYSTAL),
    )
  }

  private fun causingPlayer(direct: org.bukkit.entity.Entity?): Player? =
    when (direct) {
      is Player -> direct
      is Projectile -> direct.shooter as? Player
      is TNTPrimed -> direct.source as? Player
      else -> null
    }

  private fun multiplierFrom(
    mkacPlayer: MKACPlayer,
    direct: org.bukkit.entity.Entity?,
    channel: String,
  ): Double =
    if (direct is Projectile) {
      stamps.take(direct.uniqueId)?.multiplier ?: 1.0
    } else {
      multiplier(mkacPlayer, channel)
    }

  private fun scaleStamped(
    event: EntityDamageByEntityEvent,
    victim: Player,
    subject: java.util.UUID?,
  ) {
    val stamp = subject?.let { stamps.peek(it) } ?: return
    if (stamp.owner == victim.uniqueId) return
    val mkacPlayer = playerDataManager.getPlayer(stamp.owner) ?: return
    scale(event, mkacPlayer, MitigationSettings.CRYSTAL, stamp.multiplier)
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onHeal(event: EntityRegainHealthEvent) {
    val player = event.entity as? Player ?: return
    val mkacPlayer = playerDataManager.getPlayer(player) ?: return
    val multiplier = multiplier(mkacPlayer, MitigationSettings.HEALING)
    if (multiplier >= 1.0) return
    if (vetoed(mkacPlayer, MitigationSettings.HEALING)) return
    event.amount = event.amount * multiplier
  }

  private fun cancelled(mkacPlayer: MKACPlayer): Boolean {
    val chance = mkacPlayer.mitigation.chanceFor(MitigationSettings.CANCEL)
    if (chance <= 0.0 || random.nextDouble() >= chance) return false
    return !vetoed(mkacPlayer, MitigationSettings.CANCEL)
  }

  private fun boostIncoming(event: EntityDamageByEntityEvent, victim: Player) {
    val mkacPlayer = playerDataManager.getPlayer(victim) ?: return
    val multiplier = multiplier(mkacPlayer, MitigationSettings.INCOMING)
    if (multiplier <= 1.0) return
    if (vetoed(mkacPlayer, MitigationSettings.INCOMING)) return

    val survivable = event.finalDamage < victim.health
    event.damage = event.damage * multiplier

    if (!survivable || event.finalDamage < victim.health) return
    val room = (victim.health - SURVIVES_BY) / event.finalDamage
    event.damage = event.damage * room.coerceIn(0.0, 1.0)
  }

  private fun scale(
    event: EntityDamageByEntityEvent,
    mkacPlayer: MKACPlayer,
    channel: String,
    multiplier: Double,
  ) {
    if (multiplier >= 1.0) return
    if (vetoed(mkacPlayer, channel)) return
    event.damage = event.damage * multiplier
  }

  private fun multiplier(mkacPlayer: MKACPlayer, channel: String): Double =
    mkacPlayer.mitigation.multiplierFor(channel)

  private fun vetoed(mkacPlayer: MKACPlayer, channel: String): Boolean {
    val state = mkacPlayer.mitigation
    val event =
      MitigationEvent(
        mkacPlayer.uuid,
        mkacPlayer.player.name,
        state.applied?.id ?: channel,
        state.appliedTier.name,
        state.score,
      )
    mkacPlayer.eventBus.post(event)
    return event.cancelled
  }
}
