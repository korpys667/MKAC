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
package ru.korpys667.utils

import kotlin.math.roundToInt
import ru.korpys667.checks.impl.ai.ProbabilityTrail

object Sparkline {

  private val BLOCKS = charArrayOf('▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')

  fun of(points: ByteArray, width: Int): String {
    if (points.isEmpty() || width <= 0) return ""
    val builder = StringBuilder(width)
    val perColumn = points.size.toDouble() / width
    for (column in 0 until width) {
      val from = (column * perColumn).toInt()
      val until = minOf(points.size, ((column + 1) * perColumn).toInt().coerceAtLeast(from + 1))
      var peak = 0.0
      for (i in from until until) {
        peak = maxOf(peak, ProbabilityTrail.probabilityOf(points[i]))
      }
      builder.append(BLOCKS[peak.times(BLOCKS.size - 1).roundToInt().coerceIn(0, BLOCKS.size - 1)])
    }
    return builder.toString()
  }
}
