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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.korpys667.alert

import ru.korpys667.utils.Message

enum class AlertType(
  val permission: String,
  val enabledMessage: Message,
  val disabledMessage: Message,
) {
  REGULAR("mkac.alerts", Message.ALERTS_ENABLED, Message.ALERTS_DISABLED),
  BRAND("mkac.brand", Message.BRAND_ALERTS_ENABLED, Message.BRAND_ALERTS_DISABLED),
  SUSPICIOUS(
    "mkac.suspicious.alerts",
    Message.SUSPICIOUS_ALERTS_ENABLED,
    Message.SUSPICIOUS_ALERTS_DISABLED,
  ),
  MITIGATION(
    "mkac.mitigations.alerts",
    Message.MITIGATION_ALERTS_ENABLED,
    Message.MITIGATION_ALERTS_DISABLED,
  ),
}
