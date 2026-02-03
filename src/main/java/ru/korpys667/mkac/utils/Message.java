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
package ru.korpys667.mkac.utils;

import lombok.Getter;

@Getter
public enum Message {
  PREFIX("prefix"),
  ALERTS_ENABLED("alerts-enabled"),
  ALERTS_DISABLED("alerts-disabled"),
  ALERTS_FORMAT("alerts-format"),
  PLAYER_NOT_FOUND("player-not-found"),
  RUN_AS_PLAYER("run-as-player"),
  RELOAD_START("reload-start"),
  RELOAD_SUCCESS("reload-success"),
  BRAND_ALERTS_ENABLED("brand.alerts-enabled"),
  BRAND_ALERTS_DISABLED("brand.alerts-disabled"),
  BRAND_NOTIFICATION("brand.notification"),
  BRAND_DISCONNECT_FORGE("brand.disconnect-forge"),
  PROB_ENABLED("prob.enabled"),
  PROB_DISABLED("prob.disabled"),
  PROB_NO_DATA("prob.no-data"),
  PROB_NO_AICHECK("prob.no-aicheck"),
  PROB_FORMAT_LABEL_PROB("prob.format.label-prob"),
  PROB_FORMAT_LABEL_BUFFER("prob.format.label-buffer"),
  PROB_FORMAT_LABEL_PING("prob.format.label-ping"),
  PROB_FORMAT_SEPARATOR("prob.format.separator"),
  PROB_FORMAT_SUFFIX_PING("prob.format.suffix-ping"),
  PROFILE_NO_DATA("profile.no-data"),
  PROFILE_LINES("profile.lines"),
  HISTORY_DISABLED("history.disabled"),
  HISTORY_HEADER("history.header"),
  HISTORY_ENTRY("history.entry"),
  HISTORY_NO_VIOLATIONS("history.no-violations"),
  LOGS_HEADER("logs.header"),
  LOGS_ENTRY("logs.entry"),
  LOGS_NO_VIOLATIONS("logs.no-violations"),
  LOGS_INVALID_TIME("logs.invalid-time"),
  PUNISH_RESET_SUCCESS("punish.reset-success"),
  SUSPICIOUS_ALERTS_ENABLED("suspicious.alerts-enabled"),
  SUSPICIOUS_ALERTS_DISABLED("suspicious.alerts-disabled"),
  SUSPICIOUS_ALERT_TRIGGERED("suspicious.alert-triggered"),
  SUSPICIOUS_LIST_EMPTY("suspicious.list-empty"),
  SUSPICIOUS_LIST_HEADER("suspicious.list-header"),
  SUSPICIOUS_LIST_ENTRY("suspicious.list-entry"),
  SUSPICIOUS_TOP_NONE("suspicious.top-none"),
  SUSPICIOUS_TOP_PLAYER("suspicious.top-player"),
  STATS_LINES("stats.lines"),
  HELP_MESSAGE("help"),
  INTERNAL_ERROR("internal.error"),
  TIME_AGO("time.ago"),
  TIME_DAYS("time.days"),
  TIME_HOURS("time.hours"),
  TIME_MINUTES("time.minutes"),
  TIME_SECONDS("time.seconds");

  private final String path;

  Message(String path) {
    this.path = path;
  }
}
