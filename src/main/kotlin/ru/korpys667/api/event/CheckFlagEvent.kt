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
package ru.korpys667.api.event

import java.util.UUID

/**
 * Fired when a check flags a player.
 *
 * This event is dispatched on the calling thread.
 */
data class CheckFlagEvent(
  val playerId: UUID,
  val playerName: String,
  val checkName: String,
  val configName: String,
  val debug: String,
  val labels: Set<String> = emptySet(),
  override var cancelled: Boolean = false,
) : MKACCancellableEvent
