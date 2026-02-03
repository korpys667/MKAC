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
package ru.korpys667.mkac.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.korpys667.mkac.MKAC;

public class HologramManager {
  private final MKAC plugin;
  private final Map<UUID, Map<UUID, PlayerHologram>> viewerHolograms = new ConcurrentHashMap<>();
  private final Map<UUID, ProbabilityHistory> probabilityHistories = new ConcurrentHashMap<>();
  private BukkitTask updateTask;

  public HologramManager(MKAC plugin) {
    this.plugin = plugin;
    startUpdateTask();
  }

  public void addProbability(UUID playerUuid, double probability) {
    ProbabilityHistory history =
        probabilityHistories.computeIfAbsent(playerUuid, k -> new ProbabilityHistory());
    history.add(probability);
  }

  public ProbabilityHistory getHistory(UUID playerUuid) {
    return probabilityHistories.getOrDefault(playerUuid, new ProbabilityHistory());
  }

  public void enableHologram(Player viewer, Player target) {
    Map<UUID, PlayerHologram> holograms =
        viewerHolograms.computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>());

    if (!holograms.containsKey(target.getUniqueId())) {
      PlayerHologram hologram = new PlayerHologram(plugin, this, viewer, target);
      holograms.put(target.getUniqueId(), hologram);
      hologram.spawn();
    }
  }

  public void enableForAll(Player viewer) {
    for (Player target : Bukkit.getOnlinePlayers()) {
      if (!target.equals(viewer) && !target.hasPermission("mkac.exempt")) {
        enableHologram(viewer, target);
      }
    }
  }

  public void disableHologram(Player viewer, Player target) {
    Map<UUID, PlayerHologram> holograms = viewerHolograms.get(viewer.getUniqueId());
    if (holograms != null) {
      PlayerHologram hologram = holograms.remove(target.getUniqueId());
      if (hologram != null) {
        hologram.remove();
      }
      if (holograms.isEmpty()) {
        viewerHolograms.remove(viewer.getUniqueId());
      }
    }
  }

  public void disableForAll(Player viewer) {
    Map<UUID, PlayerHologram> holograms = viewerHolograms.remove(viewer.getUniqueId());
    if (holograms != null) {
      for (PlayerHologram hologram : holograms.values()) {
        hologram.remove();
      }
    }
  }

  public boolean isEnabled(Player viewer, Player target) {
    Map<UUID, PlayerHologram> holograms = viewerHolograms.get(viewer.getUniqueId());
    return holograms != null && holograms.containsKey(target.getUniqueId());
  }

  public void handlePlayerQuit(Player player) {
    disableForAll(player);
    probabilityHistories.remove(player.getUniqueId());

    for (Map<UUID, PlayerHologram> holograms : viewerHolograms.values()) {
      PlayerHologram hologram = holograms.remove(player.getUniqueId());
      if (hologram != null) {
        hologram.remove();
      }
    }
  }

  private void startUpdateTask() {
    updateTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  for (Map<UUID, PlayerHologram> holograms : viewerHolograms.values()) {
                    for (PlayerHologram hologram : holograms.values()) {
                      hologram.update();
                    }
                  }
                },
                1L,
                1L);
  }

  public void shutdown() {
    if (updateTask != null) {
      updateTask.cancel();
    }
    for (Map<UUID, PlayerHologram> holograms : viewerHolograms.values()) {
      for (PlayerHologram hologram : holograms.values()) {
        hologram.remove();
      }
    }
    viewerHolograms.clear();
    probabilityHistories.clear();
  }

  public static class ProbabilityHistory {
    private final Double[] probabilities = new Double[5];
    private int currentIndex = 0;
    private int count = 0;

    public void add(double probability) {
      probabilities[currentIndex] = probability;
      currentIndex = (currentIndex + 1) % 5;
      if (count < 5) {
        count++;
      }
    }

    public List<Double> getAll() {
      List<Double> result = new ArrayList<>();

      if (count == 0) {
        return result;
      }

      for (int i = 0; i < count; i++) {
        int index = (currentIndex - 1 - i + 50) % 5;
        if (probabilities[index] != null) {
          result.add(probabilities[index]);
        }
      }

      return result;
    }

    public double getAverage() {
      if (count == 0) return 0.0;

      double sum = 0.0;
      for (int i = 0; i < count; i++) {
        if (probabilities[i] != null) {
          sum += probabilities[i];
        }
      }
      return sum / count;
    }

    public boolean isEmpty() {
      return count == 0;
    }
  }

  private static class PlayerHologram {
    private final MKAC plugin;
    private final HologramManager manager;
    private final Player viewer;
    private final Player target;
    private final int entityId1;
    private final int entityId2;
    private boolean spawned = false;

    private double lastX, lastY, lastZ;
    private double targetX, targetY, targetZ;
    private static final double INTERPOLATION_SPEED = 0.3;

    public PlayerHologram(MKAC plugin, HologramManager manager, Player viewer, Player target) {
      this.plugin = plugin;
      this.manager = manager;
      this.viewer = viewer;
      this.target = target;
      this.entityId1 = generateEntityId();
      this.entityId2 = generateEntityId();

      Location loc = target.getLocation();
      this.lastX = this.targetX = loc.getX();
      this.lastY = this.targetY = loc.getY();
      this.lastZ = this.targetZ = loc.getZ();
    }

    private int generateEntityId() {
      return ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    public void spawn() {
      if (spawned || !target.isOnline() || !viewer.isOnline()) return;

      Location loc = target.getLocation().add(0, 0, 0);

      spawnArmorStand(entityId1, loc);
      spawnArmorStand(entityId2, loc.clone().subtract(0, 0.3, 0));

      spawned = true;

      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                updateEntityName(
                    entityId1, Component.text("0.95 0.90 0.85 0.80 0.75", NamedTextColor.RED));
                updateEntityName(entityId2, Component.text("AVG: 0.85000", NamedTextColor.YELLOW));
              },
              5L);
    }

    private void spawnArmorStand(int entityId, Location loc) {
      var user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
      if (user == null) return;

      WrapperPlayServerSpawnEntity spawnPacket =
          new WrapperPlayServerSpawnEntity(
              entityId,
              Optional.of(UUID.randomUUID()),
              EntityTypes.ARMOR_STAND,
              new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
              0f,
              0f,
              0f,
              0,
              Optional.empty());

      user.sendPacket(spawnPacket);

      List<EntityData<?>> metadata =
          Arrays.asList(
              new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20),
              new EntityData(5, EntityDataTypes.BOOLEAN, true),
              new EntityData(15, EntityDataTypes.BYTE, (byte) 0x11));

      user.sendPacket(new WrapperPlayServerEntityMetadata(entityId, metadata));
    }

    public void update() {
      if (!spawned || !target.isOnline() || !viewer.isOnline()) {
        remove();
        return;
      }

      ProbabilityHistory history = manager.getHistory(target.getUniqueId());

      List<Double> probs = history.getAll();
      double avg = history.getAverage();

      if (probs.isEmpty()) {
        probs = Arrays.asList(0.0);
        avg = 0.0;
      }

      Location targetLoc = target.getLocation();
      targetX = targetLoc.getX();
      targetY = targetLoc.getY() + 0.5;
      targetZ = targetLoc.getZ();

      lastX += (targetX - lastX) * INTERPOLATION_SPEED;
      lastY += (targetY - lastY) * INTERPOLATION_SPEED;
      lastZ += (targetZ - lastZ) * INTERPOLATION_SPEED;

      teleportEntity(entityId1, lastX, lastY, lastZ);
      teleportEntity(entityId2, lastX, lastY - 0.3, lastZ);

      updateEntityName(entityId1, createProbabilitiesText(probs));
      updateEntityName(entityId2, createAvgText(avg));
    }

    private Component createProbabilitiesText(List<Double> probs) {
      Component result = Component.empty();

      for (int i = 0; i < probs.size(); i++) {
        double prob = probs.get(i);
        TextColor color = getColorByProbability(prob);
        result = result.append(Component.text(String.format("%.2f", prob), color));

        if (i < probs.size() - 1) {
          result = result.append(Component.text(" ", NamedTextColor.WHITE));
        }
      }

      return result;
    }

    private Component createAvgText(double avg) {
      TextColor color = getColorByProbability(avg);
      return Component.text("AVG: ", NamedTextColor.GRAY)
          .append(Component.text(String.format("%.5f", avg), color));
    }

    private TextColor getColorByProbability(double probability) {
      if (probability > 0.9) return NamedTextColor.RED;
      if (probability > 0.5) return NamedTextColor.YELLOW;
      return NamedTextColor.GREEN;
    }

    private void teleportEntity(int entityId, double x, double y, double z) {
      var user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
      if (user == null) return;

      WrapperPlayServerEntityTeleport teleportPacket =
          new WrapperPlayServerEntityTeleport(entityId, new Vector3d(x, y, z), 0f, 0f, false);

      user.sendPacket(teleportPacket);
    }

    private void updateEntityName(int entityId, Component name) {
      var user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
      if (user == null) return;

      List<EntityData<?>> metadata =
          Arrays.asList(
              new EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(name)),
              new EntityData(3, EntityDataTypes.BOOLEAN, true));

      user.sendPacket(new WrapperPlayServerEntityMetadata(entityId, metadata));
    }

    public void remove() {
      if (!spawned) return;

      var user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
      if (user != null) {
        WrapperPlayServerDestroyEntities destroyPacket =
            new WrapperPlayServerDestroyEntities(entityId1, entityId2);
        user.sendPacket(destroyPacket);
      }

      spawned = false;
    }
  }
}
