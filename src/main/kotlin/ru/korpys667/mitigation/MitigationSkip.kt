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
@file:Suppress("ReturnCount")

package ru.korpys667.mitigation

import ru.korpys667.config.ConfigManager
import ru.korpys667.player.MKACPlayer
import ru.korpys667.region.RegionProvider

const val NO_MITIGATE_PERMISSION = "mkac.nomitigate"

enum class SkipReason {
  TURNED_OFF,
  EXEMPT,
  CHECKS_DISABLED,
  NO_MITIGATE,
  BEDROCK,
  DISABLED_REGION,
  TOO_FEW_ANSWERS,
}

class MitigationSkip(
  private val configManager: ConfigManager,
  private val regionProvider: RegionProvider,
  private val settings: () -> MitigationSettings,
) {

  fun skipReason(mkacPlayer: MKACPlayer): SkipReason? {
    val config = settings()
    if (!config.enabled) return SkipReason.TURNED_OFF

    val player = mkacPlayer.player
    val skip = config.skip

    if (mkacPlayer.exemptManager.isDisabled(player)) return SkipReason.CHECKS_DISABLED
    if (mkacPlayer.exemptManager.isExempt(player)) return SkipReason.EXEMPT
    if (player.hasPermission(NO_MITIGATE_PERMISSION)) return SkipReason.NO_MITIGATE
    if (skip.bedrock && mkacPlayer.isBedrockExempt) return SkipReason.BEDROCK
    if (
      skip.followAiRegions &&
        configManager.isAiWorldGuardEnabled() &&
        regionProvider.isPlayerInDisabledRegion(player)
    ) {
      return SkipReason.DISABLED_REGION
    }

    return null
  }
}
