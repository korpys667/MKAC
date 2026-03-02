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
package ru.korpys667.mkac.utils.latency;

import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class LatencyUtils implements ILatencyUtils {

  private record TransactionTask(int transactionId, Runnable task) {}

  private final ArrayDeque<TransactionTask> transactionMap = new ArrayDeque<>();
  private final MKPlayer player;
  private final MKAC plugin;
  private final ArrayList<Runnable> tasksToRun = new ArrayList<>();

  public LatencyUtils(MKPlayer player, MKAC plugin) {
    this.player = player;
    this.plugin = plugin;
  }

  @Override
  public void addRealTimeTask(int transaction, Runnable runnable) {
    addRealTimeTaskInternal(transaction, false, runnable);
  }

  @Override
  public void addRealTimeTaskAsync(int transaction, Runnable runnable) {
    addRealTimeTaskInternal(transaction, true, runnable);
  }

  private void addRealTimeTaskInternal(int transactionId, boolean async, Runnable runnable) {
    if (player.getLastTransactionReceived().get() >= transactionId) {
      if (async) {
        ChannelHelper.runInEventLoop(player.getUser().getChannel(), runnable);
      } else {
        runnable.run();
      }
      return;
    }
    synchronized (transactionMap) {
      transactionMap.add(new TransactionTask(transactionId, runnable));
    }
  }

  @Override
  public void handleNettySyncTransaction(int receivedTransactionId) {
    synchronized (transactionMap) {
      tasksToRun.clear();

      Iterator<TransactionTask> iterator = transactionMap.iterator();
      while (iterator.hasNext()) {
        TransactionTask taskEntry = iterator.next();
        int taskTransactionId = taskEntry.transactionId();

        if (receivedTransactionId + 1 < taskTransactionId) {
          break;
        }

        if (receivedTransactionId == taskTransactionId - 1) {
          continue;
        }

        tasksToRun.add(taskEntry.task());
        iterator.remove();
      }

      for (Runnable runnable : tasksToRun) {
        try {
          runnable.run();
        } catch (Exception e) {
          plugin
              .getLogger()
              .severe(
                  "An error occurred when running transactions for player: "
                      + player.getUser().getName());
          e.printStackTrace();
          player.disconnect(MessageUtil.getMessage(Message.INTERNAL_ERROR));
        }
      }
    }
  }
}
