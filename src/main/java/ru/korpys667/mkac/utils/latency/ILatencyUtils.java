/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.mkac.utils.latency;

public interface ILatencyUtils {
  /**
   * @param transaction
   * @param runnable
   */
  void addRealTimeTask(int transaction, Runnable runnable);

  /**
   * @param transaction
   * @param runnable
   */
  void addRealTimeTaskAsync(int transaction, Runnable runnable);

  /**
   * @param receivedTransactionId
   */
  void handleNettySyncTransaction(int receivedTransactionId);
}
