/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
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

import java.util.function.Supplier;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.config.ConfigManager;

public class AIServerProvider implements Supplier<AIServer> {

  private final MKAC plugin;
  private final ConfigManager configManager;
  private AIServer currentInstance;

  public AIServerProvider(MKAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.reload();
  }

  public void reload() {
    if (configManager.isAiEnabled()) {
      String url = configManager.getAiServerUrl();
      String key = configManager.getAiApiKey();

      if (url == null || url.isEmpty() || key == null || key.equals("API-KEY")) {
        plugin.getLogger().warning("[AICheck] AI is enabled but not configured.");
        this.currentInstance = null;
      } else {
        plugin.getLogger().info("[AICheck] AI Check loaded.");
        this.currentInstance = new AIServer(plugin, url, key);
      }
    } else {
      plugin.getLogger().info("[AICheck] AI Check disabled.");
      this.currentInstance = null;
    }
  }

  @Override
  public AIServer get() {
    return this.currentInstance;
  }
}
