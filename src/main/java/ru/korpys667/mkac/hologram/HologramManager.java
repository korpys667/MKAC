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

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    probabilityHistories
        .computeIfAbsent(playerUuid, k -> new ProbabilityHistory())
        .add(probability);
  }

  public ProbabilityHistory getHistory(UUID playerUuid) {
    return probabilityHistories.getOrDefault(playerUuid, new ProbabilityHistory());
  }

  public void enableHologram(Player viewer, Player target) {
    Map<UUID, PlayerHologram> holograms =
        viewerHolograms.computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>());

    if (!holograms.containsKey(target.getUniqueId())) {
      PlayerHologram hologram = new PlayerHologram(this, viewer, target);
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
      if (hologram != null) hologram.remove();
      if (holograms.isEmpty()) viewerHolograms.remove(viewer.getUniqueId());
    }
  }

  public void disableForAll(Player viewer) {
    Map<UUID, PlayerHologram> holograms = viewerHolograms.remove(viewer.getUniqueId());
    if (holograms != null) {
      for (PlayerHologram hologram : holograms.values()) hologram.remove();
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
      if (hologram != null) hologram.remove();
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
    if (updateTask != null) updateTask.cancel();
    for (Map<UUID, PlayerHologram> holograms : viewerHolograms.values()) {
      for (PlayerHologram hologram : holograms.values()) hologram.remove();
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
      if (count < 5) count++;
    }

    public List<Double> getAll() {
      List<Double> result = new ArrayList<>();
      if (count == 0) return result;
      for (int i = 0; i < count; i++) {
        int index = (currentIndex - 1 - i + 50) % 5;
        if (probabilities[index] != null) result.add(probabilities[index]);
      }
      return result;
    }

    public double getAverage() {
      if (count == 0) return 0.0;
      double sum = 0.0;
      for (int i = 0; i < count; i++) {
        if (probabilities[i] != null) sum += probabilities[i];
      }
      return sum / count;
    }

    public boolean isEmpty() {
      return count == 0;
    }
  }

  private static class PlayerHologram {
    private final HologramManager manager;
    private final Player viewer;
    private final Player target;
    private final String hologramId;
    private Hologram hologram;
    private boolean spawned = false;

    PlayerHologram(HologramManager manager, Player viewer, Player target) {
      this.manager = manager;
      this.viewer = viewer;
      this.target = target;
      this.hologramId =
          "mkac_"
              + viewer.getUniqueId().toString().replace("-", "")
              + "_"
              + target.getUniqueId().toString().replace("-", "");
    }

    void spawn() {
      if (spawned || !target.isOnline() || !viewer.isOnline()) return;

      Location loc =
          new Location(
              target.getWorld(),
              target.getLocation().getX(),
              target.getLocation().getY() + 3.0,
              target.getLocation().getZ());

      hologram = DHAPI.createHologram(hologramId, loc, false);
      DHAPI.addHologramLine(hologram, "&c0.00");
      DHAPI.addHologramLine(hologram, "&7AVG: &a0.00000");

      hologram.setDefaultVisibleState(false);
      hologram.setShowPlayer(viewer);

      spawned = true;
    }

    void update() {
      if (!spawned || hologram == null || !target.isOnline() || !viewer.isOnline()) {
        remove();
        return;
      }

      ProbabilityHistory history = manager.getHistory(target.getUniqueId());
      List<Double> probs = history.getAll();
      double avg = history.getAverage();

      if (probs.isEmpty()) {
        probs = Collections.singletonList(0.0);
        avg = 0.0;
      }

      Location targetLoc = target.getLocation();
      Location holoLoc =
          new Location(
              targetLoc.getWorld(), targetLoc.getX(), targetLoc.getY() + 3.0, targetLoc.getZ());
      DHAPI.moveHologram(hologram, holoLoc);

      DHAPI.setHologramLine(hologram, 0, buildProbLine(probs));
      DHAPI.setHologramLine(hologram, 1, buildAvgLine(avg));
    }

    private String buildProbLine(List<Double> probs) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < probs.size(); i++) {
        double p = probs.get(i);
        sb.append(colorCode(p)).append(String.format("%.2f", p));
        if (i < probs.size() - 1) sb.append("&f ");
      }
      return sb.toString();
    }

    private String buildAvgLine(double avg) {
      return "&7AVG: " + colorCode(avg) + String.format("%.5f", avg);
    }

    private String colorCode(double p) {
      if (p > 0.9) return "&c";
      if (p > 0.5) return "&e";
      return "&a";
    }

    void remove() {
      if (!spawned) return;
      if (hologram != null) {
        hologram.delete();
        hologram = null;
      }
      spawned = false;
    }
  }
}
