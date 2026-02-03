/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
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
package ru.korpys667.mkac.database;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.korpys667.mkac.player.MKPlayer;

public interface ViolationDatabase {
  void logAlert(MKPlayer player, String verbose, String checkName, int vls);

  int getLogCount(UUID player);

  List<Violation> getViolations(UUID player, int page, int limit);

  int getUniqueViolatorsSince(long since);

  int getLogCount(long since);

  List<Violation> getViolations(int page, int limit, long since);

  int getViolationLevel(UUID playerUUID, String punishGroupName);

  int incrementViolationLevel(UUID playerUUID, String punishGroupName);

  void resetViolationLevel(UUID playerUUID, String punishGroupName);

  void resetAllViolationLevels(UUID playerUUID);

  void saveProbability(UUID uuid, String playerName, double probability);

  List<Double> getPlayerProbabilities(UUID uuid, int limit);

  void deletePlayerProbabilities(UUID uuid);

  Map<UUID, PlayerMenuData> getAllOnlinePlayerMenuData();
}
