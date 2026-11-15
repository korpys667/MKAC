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
package ru.korpys667.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import ru.korpys667.checks.type.PacketCheck
import ru.korpys667.checks.type.RotationCheck
import ru.korpys667.checks.type.TickCheck
import ru.korpys667.player.MKACPlayer
import ru.korpys667.utils.update.RotationUpdate

class CheckManager(private val player: MKACPlayer, checkFactories: Set<CheckFactory>) {
  private val rotationChecks = ArrayList<RotationCheck>()
  private val packetChecks = ArrayList<PacketCheck>()
  private val tickChecks = ArrayList<TickCheck>()
  private val checks = HashMap<Class<out ICheck>, ICheck>()

  init {
    for (factory in checkFactories) {
      registerCheck(factory.create(player))
    }
  }

  private fun checksDisabled(): Boolean =
    player.exemptManager.isDisabled(player.player) || player.isBedrockExempt

  private fun registerCheck(check: ICheck) {
    checks[check.javaClass] = check

    if (check is RotationCheck) {
      rotationChecks.add(check)
    }

    if (check is PacketCheck) {
      packetChecks.add(check)
    }

    if (check is TickCheck) {
      tickChecks.add(check)
    }
  }

  fun reloadChecks() {
    for (check in checks.values) {
      if (check is Reloadable) {
        check.reload()
      }
    }
  }

  fun onRotationUpdate(update: RotationUpdate) {
    if (checksDisabled()) {
      return
    }
    for (check in rotationChecks) {
      check.process(update)
    }
  }

  fun onPacketReceive(event: PacketReceiveEvent) {
    if (checksDisabled()) {
      return
    }
    for (i in packetChecks.indices) {
      packetChecks[i].onPacketReceive(event)
    }
  }

  fun onDataTick(player: MKACPlayer) {
    if (checksDisabled()) {
      return
    }
    for (i in tickChecks.indices) {
      tickChecks[i].onDataTick(player)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : ICheck> getCheck(clazz: Class<T>): T? {
    return checks[clazz] as T?
  }

  fun getAllChecks(): Collection<ICheck> = checks.values

  fun interface Factory {
    fun create(player: MKACPlayer): CheckManager
  }
}
