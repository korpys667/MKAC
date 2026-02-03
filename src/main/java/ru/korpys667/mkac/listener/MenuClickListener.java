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
package ru.korpys667.mkac.listener;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.menu.ChickenCoopMenu;

public class MenuClickListener implements Listener {

  private final MKAC plugin;
  private final ChickenCoopMenu chickenCoopMenu;

  public MenuClickListener(MKAC plugin, ChickenCoopMenu chickenCoopMenu) {
    this.plugin = plugin;
    this.chickenCoopMenu = chickenCoopMenu;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;

    Player player = (Player) event.getWhoClicked();
    String title = event.getView().getTitle();

    if (!ChickenCoopMenu.isChickenCoopMenu(title)) return;

    event.setCancelled(true);

    if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
      return;
    }

    int slot = event.getRawSlot();
    UUID targetUuid = chickenCoopMenu.getPlayerUuidBySlot(slot);

    if (targetUuid == null) return;

    Player target = plugin.getServer().getPlayer(targetUuid);
    if (target == null || !target.isOnline()) {
      player.sendMessage(ChatColor.RED + "Игрок не в сети!");
      return;
    }

    if (player.getGameMode() != GameMode.SPECTATOR) {
      player.setGameMode(GameMode.SPECTATOR);
    }

    player.teleport(target.getLocation());
    player.sendMessage(
        ChatColor.GREEN + "Вы следите за игроком " + ChatColor.WHITE + target.getName());
    player.closeInventory();
  }
}
