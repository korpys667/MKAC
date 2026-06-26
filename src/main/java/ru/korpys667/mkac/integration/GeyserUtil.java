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
package ru.korpys667.mkac.integration;

import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;

public final class GeyserUtil {

  private static final String FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi";
  private static final String GEYSER_API_CLASS = "org.geysermc.geyser.api.GeyserApi";
  private static final String BEDROCK_UUID_PREFIX = "00000000-0000-0000-0009";

  private static final boolean floodgatePresent;
  private static final boolean geyserPresent;

  static {
    floodgatePresent = hasClass(FLOODGATE_API_CLASS);
    geyserPresent = hasClass(GEYSER_API_CLASS);
  }

  private GeyserUtil() {}

  public static boolean isBedrockPlayer(UUID uuid) {
    return isFloodgateBedrock(uuid)
        || isGeyserBedrock(uuid)
        || uuid.toString().startsWith(BEDROCK_UUID_PREFIX);
  }

  private static boolean isFloodgateBedrock(UUID uuid) {
    if (!floodgatePresent) return false;
    try {
      return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean isGeyserBedrock(UUID uuid) {
    if (!geyserPresent) return false;
    try {
      Class<?> geyserApiClass = Class.forName(GEYSER_API_CLASS);
      Object api = geyserApiClass.getMethod("api").invoke(null);
      return (boolean) api.getClass().getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean hasClass(String name) {
    try {
      Class.forName(name);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
