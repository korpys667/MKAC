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

import java.util.Optional
import java.util.UUID
import ru.korpys667.ai.AiService
import ru.korpys667.api.model.AiSnapshot
import ru.korpys667.api.service.AiApi
import ru.korpys667.checks.impl.ai.AiCheck
import ru.korpys667.player.PlayerDataManager

class AiApiImpl(
  private val aiService: AiService,
  private val playerDataManager: PlayerDataManager,
) : AiApi {
  override fun isEnabled(): Boolean = aiService.isEnabled

  override fun getSnapshot(playerId: UUID): Optional<AiSnapshot> {
    val mkacPlayer = playerDataManager.getPlayer(playerId) ?: return Optional.empty()
    val aiCheck = mkacPlayer.checkManager.getCheck(AiCheck::class.java) ?: return Optional.empty()
    return Optional.of(
      AiSnapshot(
        aiCheck.lastCheatProbability,
        aiCheck.buffer,
        mkacPlayer.combat.damageMultiplier,
        aiCheck.prob90,
        mkacPlayer.mitigation.appliedTier.name,
        mkacPlayer.mitigation.score,
        aiCheck.labelBufferSnapshot(),
        aiCheck.lastLabelProbabilities,
      )
    )
  }
}
