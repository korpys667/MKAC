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
package ru.korpys667.mkac.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.checks.AbstractCheck;
import ru.korpys667.mkac.checks.CheckData;
import ru.korpys667.mkac.checks.Reloadable;
import ru.korpys667.mkac.checks.type.PacketCheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.data.TickData;
import ru.korpys667.mkac.debug.DebugCategory;
import ru.korpys667.mkac.flatbuffers.TickDataSequence;
import ru.korpys667.mkac.integration.WorldGuardManager;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.server.AIResponse;
import ru.korpys667.mkac.server.AIServer;
import ru.korpys667.mkac.server.AIServerProvider;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

@CheckData(name = "AI (Aim)")
public class AICheck extends AbstractCheck implements PacketCheck, Reloadable {
  private final MKAC plugin;
  private final AIServerProvider aiServerProvider;
  private final ConfigManager configManager;
  private final WorldGuardManager worldGuardManager;
  private final AlertManager alertManager;

  private int step;
  private AIServer aiServer;
  private Deque<TickData> ticks;
  private int ticksStep = 0;

  @Getter private double buffer = 0.0;
  @Getter private double lastProbability = 0.0;

  @Getter @Setter private int prob90 = 0;
  private boolean aiDamageReductionEnabled;
  private double aiDamageReductionProb;
  private double aiDamageReductionMultiplier;

  private double flag;
  private double bufferResetOnFlag;
  private double bufferMultiplier;
  private double bufferDecrease;
  private double suspiciousAlertBuffer;

  private static final double CHEAT_PROBABILITY = 0.90;
  private static final double LEGIT_PROBABILITY = 0.10;
  private static final Gson GSON = new Gson();
  private static final ThreadLocal<FlatBufferBuilder> BUILDER =
      ThreadLocal.withInitial(() -> new FlatBufferBuilder(4096));

  public AICheck(
      MKPlayer mkPlayer,
      MKAC plugin,
      AIServerProvider aiServerProvider,
      ConfigManager configManager,
      WorldGuardManager worldGuardManager,
      AlertManager alertManager) {
    super(mkPlayer);
    this.plugin = plugin;
    this.aiServerProvider = aiServerProvider;
    this.configManager = configManager;
    this.worldGuardManager = worldGuardManager;
    this.alertManager = alertManager;
    reload();
  }

  @Override
  public void reload() {
    this.aiServer = this.aiServerProvider.get();

    if (this.ticks == null || this.ticks.size() != configManager.getAiSequence()) {
      this.ticks = new ArrayDeque<>(configManager.getAiSequence());
    }

    this.step = configManager.getAiStep();
    this.aiDamageReductionEnabled = configManager.isAiDamageReductionEnabled();
    this.aiDamageReductionProb = configManager.getAiDamageReductionProb();
    this.aiDamageReductionMultiplier = configManager.getAiDamageReductionMultiplier();

    this.flag = configManager.getAiFlag();
    this.bufferResetOnFlag = configManager.getAiResetOnFlag();
    this.bufferMultiplier = configManager.getAiBufferMultiplier();
    this.bufferDecrease = configManager.getAiBufferDecrease();
    this.suspiciousAlertBuffer = configManager.getSuspiciousAlertsBuffer();
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (aiServer == null) return;
    if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

    int sequence = configManager.getAiSequence();

    if (mkPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
      plugin
          .getDebugManager()
          .log(
              DebugCategory.PACKET_DUPLICATION,
              "Mojang failed IQ Test for: " + mkPlayer.getPlayer().getName() + ".");
      return;
    }

    if (mkPlayer.packetStateData.lastPacketWasTeleport
        || mkPlayer.packetStateData.lastPacketWasServerRotation) {
      return;
    }

    if (mkPlayer.getTicksSinceAttack() > sequence) {
      if (!ticks.isEmpty()) {
        ticks.clear();
      }
      ticksStep = 0;
      return;
    }

    ticks.addLast(new TickData(mkPlayer));
    ticksStep++;

    while (ticks.size() > sequence) {
      ticks.removeFirst();
    }

    if (ticks.size() == sequence && ticksStep >= step) {
      if (configManager.isAiWorldGuardEnabled()
          && worldGuardManager.isPlayerInDisabledRegion(mkPlayer.getPlayer())) {
        plugin
            .getDebugManager()
            .log(
                DebugCategory.WORLDGUARD,
                "Player "
                    + mkPlayer.getPlayer().getName()
                    + " is in a disabled region. Skipping AI check.");
        ticksStep = 0;
        return;
      }
      sendData();
      ticksStep = 0;
    }
  }

  private void sendData() {
    final List<TickData> data = new ArrayList<>(ticks);
    if (aiServer == null) return;

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                byte[] flatbuffer = serialize(data);
                aiServer
                    .sendRequest(flatbuffer)
                    .thenAccept(this::onResponse)
                    .exceptionally(this::onError);
              } catch (Exception e) {
                plugin
                    .getLogger()
                    .warning(
                        "[AICheck] Failed to send data for "
                            + mkPlayer.getPlayer().getName()
                            + ": "
                            + e.getMessage());
              }
            });
  }

  private void onResponse(String response) {
    try {
      AIResponse apiResponse = GSON.fromJson(response, AIResponse.class);

      if (apiResponse == null) {
        plugin
            .getLogger()
            .warning("[AICheck] API response is missing probability. Response: " + response);
        this.lastProbability = 0.0;
        mkPlayer.setDmgMultiplier(1.0);
        return;
      }

      double probability = apiResponse.probability();
      this.lastProbability = probability;

      plugin.getHologramManager().addProbability(mkPlayer.getUuid(), probability);

      plugin
          .getChickenCoopMenu()
          .addOrUpdatePlayer(mkPlayer.getUuid(), mkPlayer.getPlayer().getName(), probability);

      if (aiDamageReductionEnabled) {
        if (probability >= aiDamageReductionProb) {
          double ratio = (probability - aiDamageReductionProb) / (1.0 - aiDamageReductionProb);
          double reduction = Math.min(1.0, ratio * aiDamageReductionMultiplier);
          mkPlayer.setDmgMultiplier(1.0 - reduction);
        } else {
          mkPlayer.setDmgMultiplier(1.0);
        }
      }

      if (probability > 0.9) {
        prob90++;
      }

      double oldBuffer = this.buffer;

      if (probability > CHEAT_PROBABILITY) {
        this.buffer += (probability - CHEAT_PROBABILITY) * this.bufferMultiplier;
      } else if (probability < LEGIT_PROBABILITY) {
        this.buffer = Math.max(0, this.buffer - this.bufferDecrease);
      }

      if (this.buffer > suspiciousAlertBuffer && oldBuffer <= suspiciousAlertBuffer) {
        alertManager.send(
            MessageUtil.getMessage(
                Message.SUSPICIOUS_ALERT_TRIGGERED,
                "player",
                this.mkPlayer.getPlayer().getName(),
                "buffer",
                String.format("%.1f", this.buffer)),
            AlertType.SUSPICIOUS);
      }

      plugin
          .getDebugManager()
          .log(
              DebugCategory.AI_PROBABILITY,
              String.format(
                  "[%s] Prob: %.4f | Buffer: %.2f -> %.2f | Damage Multiplier: %.2f",
                  this.mkPlayer.getPlayer().getName(),
                  probability,
                  oldBuffer,
                  this.buffer,
                  mkPlayer.getDmgMultiplier()));

      if (this.buffer > this.flag) {
        flag(
            "prob="
                + String.format("%.2f", probability)
                + " buffer="
                + String.format("%.1f", this.buffer));
        this.buffer = this.bufferResetOnFlag;

        if (plugin.getStatsReporter() != null) {
          plugin
              .getStatsReporter()
              .reportBan(mkPlayer.getPlayer().getName(), "AI (Aim)", probability);
        }
      }

    } catch (JsonSyntaxException e) {
      plugin
          .getLogger()
          .warning(
              "[AICheck] Error parsing API response: "
                  + e.getMessage()
                  + ". Response Body: "
                  + response);
      this.lastProbability = 0.0;
      mkPlayer.setDmgMultiplier(1.0);
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("[AICheck] Unexpected error processing API response: " + e.getMessage());
      e.printStackTrace();
      this.lastProbability = 0.0;
      mkPlayer.setDmgMultiplier(1.0);
    }
  }

  private Void onError(Throwable error) {
    this.lastProbability = 0.0;
    mkPlayer.setDmgMultiplier(1.0);

    Throwable cause =
        (error instanceof java.util.concurrent.CompletionException && error.getCause() != null)
            ? error.getCause()
            : error;

    if (cause instanceof AIServer.RequestException e) {
      if (e.getCode() == AIServer.ResponseCode.WAITING) {
        return null;
      }
      if (e.getCode() == AIServer.ResponseCode.INVALID_SEQUENCE) {
        try {
          JsonObject json = GSON.fromJson(e.getMessage().split(": ", 2)[1], JsonObject.class);
          if (json.has("details") && json.get("details").isJsonObject()) {
            JsonObject details = json.getAsJsonObject("details");
            if (details.has("sequence")) {
              int newSequence = details.get("sequence").getAsInt();
              if (configManager.getAiSequence() != newSequence) {
                plugin.getLogger().info("[AICheck] Received new sequence length " + newSequence);
                configManager.setAiSequence(newSequence);
                this.ticks = new ArrayDeque<>(newSequence);
              }
              return null;
            }
          }
        } catch (Exception parseEx) {
          plugin
              .getLogger()
              .warning("[AICheck] Failed to parse correct sequence: " + parseEx.getMessage());
        }
      }

      String logMessage =
          "[AICheck] API Error "
              + e.getCode()
              + " for player "
              + mkPlayer.getPlayer().getName()
              + ": "
              + e.getMessage();

      if (e.getCode() == AIServer.ResponseCode.TIMEOUT) {
        plugin.getDebugManager().log(DebugCategory.AI_TIMEOUT, logMessage);
      } else {
        plugin.getLogger().warning(logMessage);
      }
    } else {
      plugin
          .getLogger()
          .warning(
              "[AICheck] Unknown API Error for "
                  + mkPlayer.getPlayer().getName()
                  + ": "
                  + cause.getMessage());
    }
    return null;
  }

  private byte[] serialize(List<TickData> ticks) {
    final FlatBufferBuilder builder = BUILDER.get();

    builder.clear();

    int[] tickOffsets = new int[ticks.size()];

    for (int i = ticks.size() - 1; i >= 0; i--) {
      TickData tick = ticks.get(i);
      ru.korpys667.mkac.flatbuffers.TickData.startTickData(builder);
      ru.korpys667.mkac.flatbuffers.TickData.addDeltaYaw(builder, tick.deltaYaw);
      ru.korpys667.mkac.flatbuffers.TickData.addDeltaPitch(builder, tick.deltaPitch);
      ru.korpys667.mkac.flatbuffers.TickData.addAccelYaw(builder, tick.accelYaw);
      ru.korpys667.mkac.flatbuffers.TickData.addAccelPitch(builder, tick.accelPitch);
      ru.korpys667.mkac.flatbuffers.TickData.addJerkYaw(builder, tick.jerkYaw);
      ru.korpys667.mkac.flatbuffers.TickData.addJerkPitch(builder, tick.jerkPitch);
      ru.korpys667.mkac.flatbuffers.TickData.addGcdErrorYaw(builder, tick.gcdErrorYaw);
      ru.korpys667.mkac.flatbuffers.TickData.addGcdErrorPitch(builder, tick.gcdErrorPitch);
      tickOffsets[i] = ru.korpys667.mkac.flatbuffers.TickData.endTickData(builder);
    }

    int ticksVector = TickDataSequence.createTicksVector(builder, tickOffsets);
    TickDataSequence.startTickDataSequence(builder);
    TickDataSequence.addTicks(builder, ticksVector);
    int sequenceOffset = TickDataSequence.endTickDataSequence(builder);
    builder.finish(sequenceOffset);

    ByteBuffer buf = builder.dataBuffer();

    byte[] bytes = new byte[buf.remaining()];
    buf.get(bytes);
    return bytes;
  }
}
