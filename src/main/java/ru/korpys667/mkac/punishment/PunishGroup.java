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
package ru.korpys667.mkac.punishment;

import java.util.*;
import lombok.Getter;
import ru.korpys667.mkac.checks.ICheck;

@Getter
public class PunishGroup {
  private final String groupName;
  private final Set<String> associatedCheckNames;
  private final NavigableMap<Integer, List<String>> actions;

  public PunishGroup(
      String groupName, List<String> checkNames, NavigableMap<Integer, List<String>> actions) {
    this.groupName = groupName;
    this.associatedCheckNames =
        new HashSet<>(checkNames.stream().map(String::toLowerCase).toList());
    this.actions = actions;
  }

  public boolean isCheckAssociated(ICheck check) {
    String checkNameLower = check.getCheckName().toLowerCase(Locale.ROOT);
    for (String filter : associatedCheckNames) {
      if (checkNameLower.contains(filter)) {
        return true;
      }
    }
    return false;
  }
}
