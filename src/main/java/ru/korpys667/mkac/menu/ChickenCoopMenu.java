package ru.korpys667.mkac.menu;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.database.PlayerMenuData;

public class ChickenCoopMenu {
  private static final int MENU_SIZE = 54;
  private static final String MENU_TITLE = "Курятник";
  private static final int MAX_PROBABILITIES = 10;

  private final MKAC plugin;
  private final LinkedHashMap<UUID, PlayerRiskData> playerRisks = new LinkedHashMap<>();

  public ChickenCoopMenu(MKAC plugin) {
    this.plugin = plugin;
    loadFromDatabase();
  }

  private void loadFromDatabase() {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              Map<UUID, PlayerMenuData> data =
                  plugin.getDatabaseManager().getDatabase().getAllOnlinePlayerMenuData();

              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        for (PlayerMenuData menuData : data.values()) {
                          PlayerRiskData riskData =
                              new PlayerRiskData(menuData.getUuid(), menuData.getPlayerName());
                          for (Double prob : menuData.getProbabilities()) {
                            riskData.addProbability(prob);
                          }
                          playerRisks.put(menuData.getUuid(), riskData);
                        }
                      });
            });
  }

  public void addOrUpdatePlayer(UUID uuid, String playerName, double probability) {
    PlayerRiskData data = playerRisks.get(uuid);

    if (data == null) {
      data = new PlayerRiskData(uuid, playerName);
      playerRisks.put(uuid, data);
    }

    data.addProbability(probability);

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              plugin
                  .getDatabaseManager()
                  .getDatabase()
                  .saveProbability(uuid, playerName, probability);
            });

    if (playerRisks.size() > MENU_SIZE) {
      Iterator<UUID> iterator = playerRisks.keySet().iterator();
      if (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    }
  }

  public void removePlayer(UUID uuid) {
    playerRisks.remove(uuid);
  }

  public void restorePlayer(UUID uuid, String playerName) {
    if (playerRisks.containsKey(uuid)) {
      return;
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              List<Double> probs =
                  plugin
                      .getDatabaseManager()
                      .getDatabase()
                      .getPlayerProbabilities(uuid, MAX_PROBABILITIES);

              if (!probs.isEmpty()) {
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          PlayerRiskData data = new PlayerRiskData(uuid, playerName);
                          for (Double prob : probs) {
                            data.addProbability(prob);
                          }
                          playerRisks.put(uuid, data);
                        });
              }
            });
  }

  public void openMenu(Player viewer) {
    Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

    List<PlayerRiskData> sortedPlayers =
        playerRisks.values().stream()
            .filter(data -> Bukkit.getPlayer(data.getUuid()) != null)
            .sorted(
                (p1, p2) -> Double.compare(p2.getAverageProbability(), p1.getAverageProbability()))
            .collect(Collectors.toList());

    int slot = 0;
    for (PlayerRiskData data : sortedPlayers) {
      if (slot >= MENU_SIZE) break;

      ItemStack wool = createWoolItem(data);
      inventory.setItem(slot, wool);
      slot++;
    }

    viewer.openInventory(inventory);
  }

  private ItemStack createWoolItem(PlayerRiskData data) {
    double avgProbability = data.getAverageProbability();
    Material woolType = getWoolByProbability(avgProbability);

    ItemStack wool = new ItemStack(woolType);
    ItemMeta meta = wool.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(ChatColor.WHITE + data.getPlayerName());

      List<String> lore = new ArrayList<>();
      lore.add("");
      lore.add(ChatColor.GRAY + "Последние проверки:");

      List<Double> probs = data.getLastProbabilities();

      if (probs.isEmpty()) {
        lore.add(ChatColor.GRAY + "Нет данных");
      } else if (probs.size() <= 5) {
        lore.add(formatLastProbabilitiesWithColors(probs));
      } else {
        lore.add(formatLastProbabilitiesWithColors(probs.subList(0, 5)));
        lore.add(formatLastProbabilitiesWithColors(probs.subList(5, Math.min(10, probs.size()))));
      }

      lore.add("");
      lore.add(ChatColor.GRAY + "Средний риск:");

      ChatColor avgColor = getColorByProbability(avgProbability);
      lore.add(avgColor + "AVG " + String.format("%.2f", avgProbability));

      lore.add("");
      lore.add(ChatColor.GREEN + "Нажмите, чтобы следить");

      meta.setLore(lore);
      wool.setItemMeta(meta);
    }

    return wool;
  }

  private Material getWoolByProbability(double probability) {
    if (probability > 0.9) {
      return Material.RED_WOOL;
    } else if (probability > 0.5) {
      return Material.YELLOW_WOOL;
    } else {
      return Material.LIME_WOOL;
    }
  }

  private ChatColor getColorByProbability(double probability) {
    if (probability > 0.9) {
      return ChatColor.RED;
    } else if (probability > 0.5) {
      return ChatColor.YELLOW;
    } else {
      return ChatColor.GREEN;
    }
  }

  private String formatLastProbabilitiesWithColors(List<Double> probabilities) {
    if (probabilities.isEmpty()) {
      return ChatColor.GRAY + "Нет данных";
    }

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < probabilities.size(); i++) {
      double prob = probabilities.get(i);
      ChatColor color = getColorByProbability(prob);
      result.append(color).append(String.format("%.2f", prob));

      if (i < probabilities.size() - 1) {
        result.append(ChatColor.GRAY).append(", ");
      }
    }

    return result.toString();
  }

  public UUID getPlayerUuidBySlot(int slot) {
    if (slot < 0 || slot >= MENU_SIZE) return null;

    List<PlayerRiskData> sortedPlayers =
        playerRisks.values().stream()
            .filter(data -> Bukkit.getPlayer(data.getUuid()) != null)
            .sorted(
                (p1, p2) -> Double.compare(p2.getAverageProbability(), p1.getAverageProbability()))
            .collect(Collectors.toList());

    if (slot >= sortedPlayers.size()) return null;

    return sortedPlayers.get(slot).getUuid();
  }

  public void clear() {
    playerRisks.clear();
  }

  public static boolean isChickenCoopMenu(String title) {
    return MENU_TITLE.equals(title);
  }

  private static class PlayerRiskData {
    private final UUID uuid;
    private final String playerName;
    private final LinkedList<Double> probabilities = new LinkedList<>();

    public PlayerRiskData(UUID uuid, String playerName) {
      this.uuid = uuid;
      this.playerName = playerName;
    }

    public void addProbability(double probability) {
      probabilities.addLast(probability);
      if (probabilities.size() > MAX_PROBABILITIES) {
        probabilities.removeFirst();
      }
    }

    public List<Double> getLastProbabilities() {
      List<Double> reversed = new ArrayList<>(probabilities);
      Collections.reverse(reversed);
      return reversed;
    }

    public double getAverageProbability() {
      if (probabilities.isEmpty()) return 0.0;
      return probabilities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public UUID getUuid() {
      return uuid;
    }

    public String getPlayerName() {
      return playerName;
    }
  }
}
