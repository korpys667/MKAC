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
package ru.korpys667.mkac.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import ru.korpys667.mkac.checks.AbstractCheck;
import ru.korpys667.mkac.checks.CheckData;
import ru.korpys667.mkac.checks.type.PacketCheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.entity.PacketEntity;
import ru.korpys667.mkac.player.MKPlayer;

@CheckData(name = "ActionManager_Internal")
public class ActionManager extends AbstractCheck implements PacketCheck {

  public ActionManager(MKPlayer player, ConfigManager configManager) {
    super(player);
    int sequence = configManager.getAiSequence();
    player.ticksSinceAttack = sequence + 1;
  }

  @Override
  public void onPacketReceive(final PacketReceiveEvent event) {
    if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
      WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
      if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
        PacketEntity entity = mkPlayer.getCompensatedEntities().getEntity(action.getEntityId());

        if (entity == null || entity.isPlayer) {
          mkPlayer.ticksSinceAttack = 0;
        }
      }
    } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
      mkPlayer.ticksSinceAttack++;
    }
  }
}
