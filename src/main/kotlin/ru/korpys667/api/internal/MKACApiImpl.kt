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
package ru.korpys667.api.internal

import ru.korpys667.api.MKACApi
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.api.service.AiApi
import ru.korpys667.api.service.CheckApi
import ru.korpys667.api.service.MonitorApi
import ru.korpys667.api.service.PunishmentApi

class MKACApiImpl(
  private val aiApi: AiApi,
  private val checkApi: CheckApi,
  private val punishmentApi: PunishmentApi,
  private val monitorApi: MonitorApi,
  private val eventBus: MKACEventBus,
) : MKACApi {
  override fun ai(): AiApi = aiApi

  override fun checks(): CheckApi = checkApi

  override fun punishments(): PunishmentApi = punishmentApi

  override fun monitor(): MonitorApi = monitorApi

  override fun events(): MKACEventBus = eventBus
}
