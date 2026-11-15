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
package ru.korpys667.api

import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.api.service.AiApi
import ru.korpys667.api.service.CheckApi
import ru.korpys667.api.service.MonitorApi
import ru.korpys667.api.service.PunishmentApi

/**
 * Public API surface for MKAC.
 *
 * Obtain an instance via [ru.korpys667.api.MKACApiProvider.get].
 */
interface MKACApi {
  /** AI-related data access and status. */
  fun ai(): AiApi

  /** Check metadata and per-player check listing. */
  fun checks(): CheckApi

  /** Violation and punishment accessors. */
  fun punishments(): PunishmentApi

  /** Current monitor snapshot data (probability/buffer/ping/dmg). */
  fun monitor(): MonitorApi

  /** MKAC event bus for subscribing to internal events. */
  fun events(): MKACEventBus
}
