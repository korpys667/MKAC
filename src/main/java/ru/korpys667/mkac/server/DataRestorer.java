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
package ru.korpys667.mkac.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import ru.korpys667.mkac.data.TickData;

public class DataRestorer {

  private final JavaPlugin plugin;
  private final File restoredDataFolder;

  public DataRestorer(JavaPlugin plugin) {
    this.plugin = plugin;
    this.restoredDataFolder = new File(plugin.getDataFolder(), "restored_data");
    if (!restoredDataFolder.exists()) {
      restoredDataFolder.mkdirs();
    }
  }

  /**
   * @param playerName
   * @param history
   * @return
   */
  public boolean restoreData(String playerName, List<TickData> history) {
    if (history == null || history.isEmpty()) {
      return false;
    }

    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String fileName = playerName + "_" + timestamp + ".csv";
    File file = new File(restoredDataFolder, fileName);

    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      writer.println(
          "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch");

      for (TickData tick : history) {
        writer.println(
            "0,"
                + String.format("%.6f", tick.deltaYaw)
                + ","
                + String.format("%.6f", tick.deltaPitch)
                + ","
                + String.format("%.6f", tick.accelYaw)
                + ","
                + String.format("%.6f", tick.accelPitch)
                + ","
                + String.format("%.6f", tick.jerkYaw)
                + ","
                + String.format("%.6f", tick.jerkPitch)
                + ","
                + String.format("%.6f", tick.gcdErrorYaw)
                + ","
                + String.format("%.6f", tick.gcdErrorPitch));
      }
      return true;
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to restore data for " + playerName, e);
      return false;
    }
  }

  public File getRestoredDataFolder() {
    return restoredDataFolder;
  }
}
