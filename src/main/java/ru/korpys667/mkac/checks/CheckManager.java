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
package ru.korpys667.mkac.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import java.util.*;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.checks.impl.ai.AICheck;
import ru.korpys667.mkac.checks.impl.ai.ActionManager;
import ru.korpys667.mkac.checks.impl.aim.AimProcessor;
import ru.korpys667.mkac.checks.impl.misc.ClientBrand;
import ru.korpys667.mkac.checks.type.PacketCheck;
import ru.korpys667.mkac.checks.type.RotationCheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.integration.WorldGuardManager;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.server.AIServerProvider;
import ru.korpys667.mkac.utils.update.RotationUpdate;

public class CheckManager {
  private final List<RotationCheck> rotationChecks = new ArrayList<>();
  private final List<PacketCheck> packetChecks = new ArrayList<>();

  private final Map<Class<? extends ICheck>, ICheck> checks = new HashMap<>();

  public CheckManager(
      MKPlayer player,
      MKAC plugin,
      ConfigManager configManager,
      AIServerProvider aiServerProvider,
      WorldGuardManager worldGuardManager,
      AlertManager alertManager) {

    registerCheck(new AimProcessor(player));
    registerCheck(new ActionManager(player, configManager));
    registerCheck(
        new AICheck(
            player, plugin, aiServerProvider, configManager, worldGuardManager, alertManager));
    registerCheck(new ClientBrand(player, configManager, alertManager));
  }

  private void registerCheck(ICheck check) {
    checks.put(check.getClass(), check);

    if (check instanceof RotationCheck rotationCheck) {
      rotationChecks.add(rotationCheck);
    }

    if (check instanceof PacketCheck packetCheck) {
      packetChecks.add(packetCheck);
    }
  }

  public void reloadChecks() {
    for (ICheck check : checks.values()) {
      if (check instanceof Reloadable reloadable) {
        reloadable.reload();
      }
    }
  }

  public void onRotationUpdate(RotationUpdate update) {
    for (RotationCheck check : rotationChecks) {
      check.process(update);
    }
  }

  public void onPacketReceive(PacketReceiveEvent event) {
    for (PacketCheck check : packetChecks) {
      check.onPacketReceive(event);
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends ICheck> T getCheck(Class<T> clazz) {
    return (T) checks.get(clazz);
  }

  public Collection<ICheck> getAllChecks() {
    return checks.values();
  }
}
