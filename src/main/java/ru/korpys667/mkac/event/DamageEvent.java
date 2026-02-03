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
package ru.korpys667.mkac.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;

public class DamageEvent implements Listener {
  private final PlayerDataManager playerDataManager;

  public DamageEvent(PlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager)) {
      return;
    }
    MKPlayer mkPlayer = playerDataManager.getPlayer(damager);
    if (mkPlayer == null) {
      return;
    }
    double multiplier = mkPlayer.getDmgMultiplier();
    if (multiplier < 1.0) {
      event.setDamage(event.getDamage() * multiplier);
    }
  }
}
