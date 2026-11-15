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
package ru.korpys667.world

import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import ru.korpys667.player.MKACPlayer

class CompensatedWorld(private val player: MKACPlayer) {
  private val chunks = Long2ObjectOpenHashMap<Array<BaseChunk?>>()
  private var minSectionYOverride: Int? = null

  fun onChunkLoad(chunkX: Int, chunkZ: Int, sections: Array<out BaseChunk?>) {
    chunks.put(chunkKey(chunkX, chunkZ), Array(sections.size) { sections[it] })
  }

  fun onChunkUnload(chunkX: Int, chunkZ: Int) {
    chunks.remove(chunkKey(chunkX, chunkZ))
  }

  fun updateMinHeight(minY: Int) {
    minSectionYOverride = minY shr 4
  }

  fun setBlock(x: Int, y: Int, z: Int, state: WrappedBlockState?) {
    val sections = chunks.get(chunkKey(x shr 4, z shr 4)) ?: return
    val sectionIndex = (y shr 4) - minSectionY()
    if (sectionIndex < 0 || sectionIndex >= sections.size) return
    val section = sections[sectionIndex] ?: return
    if (state != null) section.set(x and 0xF, y and 0xF, z and 0xF, state.globalId)
  }

  fun getBlock(x: Int, y: Int, z: Int): WrappedBlockState? {
    val sections = chunks.get(chunkKey(x shr 4, z shr 4)) ?: return null
    val sectionIndex = (y shr 4) - minSectionY()
    if (sectionIndex < 0 || sectionIndex >= sections.size) return null
    val section = sections[sectionIndex] ?: return null
    return section.get(x and 0xF, y and 0xF, z and 0xF)
  }

  fun clear() {
    chunks.clear()
  }

  private fun minSectionY(): Int {
    return minSectionYOverride
      ?: if (player.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_18)) -4 else 0
  }

  companion object {
    private fun chunkKey(chunkX: Int, chunkZ: Int): Long =
      (chunkX.toLong() and 0xFFFFFFFFL) or ((chunkZ.toLong() and 0xFFFFFFFFL) shl 32)
  }
}
