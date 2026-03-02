/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
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
package ru.korpys667.mkac.alert;

import lombok.Getter;
import ru.korpys667.mkac.utils.Message;

@Getter
public enum AlertType {
  REGULAR("mkac.alerts", Message.ALERTS_ENABLED, Message.ALERTS_DISABLED),
  BRAND("mkac.brand", Message.BRAND_ALERTS_ENABLED, Message.BRAND_ALERTS_DISABLED),
  SUSPICIOUS(
      "mkac.suspicious.alerts",
      Message.SUSPICIOUS_ALERTS_ENABLED,
      Message.SUSPICIOUS_ALERTS_DISABLED);

  private final String permission;
  private final Message enabledMessage;
  private final Message disabledMessage;

  AlertType(String permission, Message enabledMessage, Message disabledMessage) {
    this.permission = permission;
    this.enabledMessage = enabledMessage;
    this.disabledMessage = disabledMessage;
  }
}
