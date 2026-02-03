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
package ru.korpys667.mkac.data;

import ru.korpys667.mkac.checks.impl.aim.AimProcessor;
import ru.korpys667.mkac.player.MKPlayer;

public class TickData {
  public final float deltaYaw, deltaPitch;
  public final float accelYaw, accelPitch;
  public final float jerkPitch, jerkYaw;
  public final float gcdErrorYaw, gcdErrorPitch;

  public TickData(MKPlayer mkPlayer) {
    AimProcessor aimProcessor = mkPlayer.getCheckManager().getCheck(AimProcessor.class);

    this.deltaYaw = mkPlayer.yaw - mkPlayer.lastYaw;
    this.deltaPitch = mkPlayer.pitch - mkPlayer.lastPitch;

    this.accelYaw = aimProcessor.getCurrentYawAccel();
    this.accelPitch = aimProcessor.getCurrentPitchAccel();

    this.jerkYaw = this.accelYaw - aimProcessor.getLastYawAccel();
    this.jerkPitch = this.accelPitch - aimProcessor.getLastPitchAccel();

    if (aimProcessor.getModeX() > 0) {
      double errorX = Math.abs(this.deltaYaw % aimProcessor.getModeX());
      this.gcdErrorYaw = (float) Math.min(errorX, aimProcessor.getModeX() - errorX);
    } else {
      this.gcdErrorYaw = 0;
    }
    if (aimProcessor.getModeY() > 0) {
      double errorY = Math.abs(this.deltaPitch % aimProcessor.getModeY());
      this.gcdErrorPitch = (float) Math.min(errorY, aimProcessor.getModeY() - errorY);
    } else {
      this.gcdErrorPitch = 0;
    }
  }
}
