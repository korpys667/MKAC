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
package ru.korpys667.debug

enum class DebugCategory(val configKey: String) {
  AI_PROBABILITY("probability"),
  AI_API_TIMEOUT("api-error.timeout"),
  AI_API_NETWORK("api-error.network"),
  AI_API_RATE_LIMITED("api-error.rate-limited"),
  AI_API_SERVICE_UNAVAILABLE("api-error.service-unavailable"),
  AI_PERSISTENT_BUFFER("persistent-buffer"),
  AI_FLOOD("flood"),
  WORLDGUARD("worldguard"),
  PACKET_DUPLICATION("packet-duplication"),
  MITIGATION("mitigation"),
}
