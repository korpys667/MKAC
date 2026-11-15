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
package ru.korpys667.redis

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class SuspiciousSnapshot
@JsonCreator
constructor(
  @param:JsonProperty("server") val server: String,
  @param:JsonProperty("uuid") val uuid: String,
  @param:JsonProperty("name") val name: String,
  @param:JsonProperty("buffer") val buffer: Double,
  @param:JsonProperty("ping") val ping: Int,
  @param:JsonProperty("updatedAt") val updatedAt: Long,
  @param:JsonProperty("level") val level: String? = null,
  @param:JsonProperty("score") val score: Double = 0.0,
)
