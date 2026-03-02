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
package ru.korpys667.mkac.command;

import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.database.DatabaseManager;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.sender.SenderFactory;

public class CommandManager {

  public CommandManager(
      MKAC plugin,
      AlertManager alertManager,
      DatabaseManager databaseManager,
      ConfigManager configManager,
      LocaleManager localeManager,
      PlayerDataManager playerDataManager) {

    LegacyPaperCommandManager<Sender> cloudManager = setupCloud(plugin);
    if (cloudManager != null) {
      CommandRegister.registerCommands(
          cloudManager,
          plugin,
          alertManager,
          databaseManager,
          configManager,
          localeManager,
          playerDataManager);
    }
  }

  private LegacyPaperCommandManager<Sender> setupCloud(MKAC plugin) {
    SenderFactory senderFactory = new SenderFactory(plugin);
    LegacyPaperCommandManager<Sender> manager;
    try {
      manager =
          new LegacyPaperCommandManager<>(
              plugin, ExecutionCoordinator.simpleCoordinator(), senderFactory);
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to initialize Cloud Command Manager: " + e.getMessage());
      e.printStackTrace();
      return null;
    }

    if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
      manager.registerBrigadier();
    } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
      manager.registerAsynchronousCompletions();
    }

    return manager;
  }
}
