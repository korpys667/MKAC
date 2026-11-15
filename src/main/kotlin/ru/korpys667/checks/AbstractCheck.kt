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

import ru.korpys667.api.event.CheckFlagEvent
import ru.korpys667.player.MKACPlayer

abstract class AbstractCheck(protected val mkacPlayer: MKACPlayer) : ICheck {
  private val checkNameInternal: String
  val configName: String

  override val checkName: String
    get() = checkNameInternal

  private val legacyCheckNamesInternal: List<String>

  override val legacyCheckNames: List<String>
    get() = legacyCheckNamesInternal

  init {
    val data = javaClass.getAnnotation(CheckData::class.java)
    checkNameInternal = data.name
    configName = if (data.configName == "DEFAULT") data.name else data.configName
    legacyCheckNamesInternal = data.legacyNames.toList()
  }

  protected fun flag(debug: String, labels: Set<String> = emptySet()) {
    val event =
      CheckFlagEvent(
        mkacPlayer.uuid,
        mkacPlayer.player.name,
        checkName,
        configName,
        debug,
        labels,
      )
    mkacPlayer.eventBus.post(event)
    if (event.cancelled) {
      return
    }
    mkacPlayer.punishmentManager.handleFlag(this, labels, debug)
  }
}
