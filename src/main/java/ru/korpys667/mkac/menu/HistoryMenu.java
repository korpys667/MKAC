package ru.korpys667.mkac.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.database.ProbabilityEntry;
import ru.korpys667.mkac.database.ViolationDatabase;

public class HistoryMenu {

  private static final int MENU_SIZE = 54;
  private static final int CONTENT_SLOTS = 45;
  private static final int ENTRIES_PER_PAGE = CONTENT_SLOTS;
  private static final int PROBS_PER_ENTRY = 10;
  private static final int MAX_PAGES = 3;

  private static final String MENU_PREFIX = ChatColor.DARK_GRAY + "История ";

  private static final String PREV_ARROW_NAME = ChatColor.GREEN + "← Предыдущая страница";
  private static final String NEXT_ARROW_NAME = ChatColor.GREEN + "Следующая страница →";

  private final MKAC plugin;
  private final Map<UUID, HistorySession> activeSessions = new ConcurrentHashMap<>();

  public HistoryMenu(MKAC plugin) {
    this.plugin = plugin;
  }

  public void open(Player viewer, String targetName, UUID targetUuid, int page) {
    ViolationDatabase db = plugin.getDatabaseManager().getDatabase();

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              int totalProbs = db.getPlayerProbabilityCount(targetUuid);
              int maxEntries = MAX_PAGES * ENTRIES_PER_PAGE;
              int maxProbs = maxEntries * PROBS_PER_ENTRY;
              int probsToLoad = Math.min(totalProbs, maxProbs);

              List<List<ProbabilityEntry>> batches = new ArrayList<>();
              int loaded = 0;

              while (loaded < probsToLoad) {
                List<ProbabilityEntry> entries =
                    db.getPlayerProbabilityEntries(targetUuid, PROBS_PER_ENTRY, loaded);
                if (entries.isEmpty()) break;
                batches.add(entries);
                loaded += entries.size();
                if (entries.size() < PROBS_PER_ENTRY) break;
              }

              int computedTotalPages =
                  Math.max(1, (int) Math.ceil((double) batches.size() / ENTRIES_PER_PAGE));
              if (computedTotalPages > MAX_PAGES) computedTotalPages = MAX_PAGES;

              int clampedPage = page;
              if (clampedPage < 1) clampedPage = 1;
              if (clampedPage > computedTotalPages) clampedPage = computedTotalPages;

              final int currentPage = clampedPage;
              final int finalTotalPages = computedTotalPages;

              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        activeSessions.put(
                            viewer.getUniqueId(),
                            new HistorySession(targetUuid, targetName, currentPage));

                        Inventory inv =
                            createInventory(
                                targetName, targetUuid, currentPage, finalTotalPages, batches);
                        viewer.openInventory(inv);
                      });
            });
  }

  private Inventory createInventory(
      String targetName,
      UUID targetUuid,
      int currentPage,
      int totalPages,
      List<List<ProbabilityEntry>> allBatches) {

    String title = MENU_PREFIX + ChatColor.DARK_GRAY + targetName;
    Inventory inv = Bukkit.createInventory(null, MENU_SIZE, title);

    int startIdx = (currentPage - 1) * ENTRIES_PER_PAGE;
    int endIdx = Math.min(startIdx + ENTRIES_PER_PAGE, allBatches.size());

    int slot = 0;
    for (int i = startIdx; i < endIdx; i++) {
      List<ProbabilityEntry> batch = allBatches.get(i);
      inv.setItem(slot, createHistoryPane(batch));
      slot++;
    }

    fillNavigation(inv, currentPage, totalPages);
    return inv;
  }

  private ItemStack createHistoryPane(List<ProbabilityEntry> entries) {
    double avg = entries.stream().mapToDouble(ProbabilityEntry::probability).average().orElse(0.0);
    ChatColor avgColor = getColorByProbability(avg);

    ItemStack glass = new ItemStack(getGlassByProbability(avg));
    ItemMeta meta = glass.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(avgColor + "AVG: " + String.format("%.4f", avg));

      List<String> lore = new ArrayList<>();
      lore.add("");

      lore.add(
          ChatColor.WHITE
              + "Вер. "
              + ChatColor.DARK_GRAY
              + "   |"
              + ChatColor.WHITE
              + " Сервер"
              + ChatColor.DARK_GRAY
              + "    |"
              + ChatColor.WHITE
              + " Время");

      lore.add(ChatColor.DARK_GRAY + "──────────────────");

      for (ProbabilityEntry entry : entries) {
        ChatColor probColor = getColorByProbability(entry.probability());
        long elapsed = System.currentTimeMillis() - entry.createdAt();
        String timeStr = formatElapsed(elapsed);
        String serverName = entry.server();
        lore.add(
            probColor
                + String.format("%.4f", entry.probability())
                + " "
                + ChatColor.GRAY
                + "|"
                + ChatColor.GRAY
                + " "
                + ChatColor.GRAY
                + serverName
                + " "
                + ChatColor.GRAY
                + "|"
                + ChatColor.GRAY
                + " "
                + timeStr);
      }

      meta.setLore(lore);
      glass.setItemMeta(meta);
    }

    return glass;
  }

  private void fillNavigation(Inventory inv, int currentPage, int totalPages) {
    if (currentPage == 1) {
      for (int i = 45; i <= 52; i++) {
        inv.setItem(i, createGlassPane());
      }
      if (totalPages > 1) {
        inv.setItem(53, createArrowItem(false));
      } else {
        inv.setItem(53, createGlassPane());
      }
    } else if (currentPage == totalPages) {
      inv.setItem(45, createArrowItem(true));
      for (int i = 46; i <= 53; i++) {
        inv.setItem(i, createGlassPane());
      }
    } else {
      inv.setItem(45, createArrowItem(true));
      for (int i = 46; i <= 52; i++) {
        inv.setItem(i, createGlassPane());
      }
      inv.setItem(53, createArrowItem(false));
    }
  }

  private ItemStack createGlassPane() {
    ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta meta = pane.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(" ");
      pane.setItemMeta(meta);
    }
    return pane;
  }

  private ItemStack createArrowItem(boolean previous) {
    ItemStack item = new ItemStack(Material.ARROW);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(previous ? PREV_ARROW_NAME : NEXT_ARROW_NAME);
      item.setItemMeta(meta);
    }
    return item;
  }

  private Material getGlassByProbability(double probability) {
    if (probability > 0.9) return Material.RED_STAINED_GLASS_PANE;
    if (probability > 0.5) return Material.YELLOW_STAINED_GLASS_PANE;
    return Material.LIME_STAINED_GLASS_PANE;
  }

  private ChatColor getColorByProbability(double probability) {
    if (probability > 0.9) return ChatColor.RED;
    if (probability > 0.5) return ChatColor.YELLOW;
    return ChatColor.GREEN;
  }

  private String formatElapsed(long millis) {
    if (millis < 0) return "0ч. 0м.";
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
    return hours + "ч. " + minutes + "м.";
  }

  /**
   * @return
   */
  public boolean handleClick(Player viewer, int rawSlot) {
    HistorySession session = activeSessions.get(viewer.getUniqueId());
    if (session == null) return false;

    if (rawSlot < 0 || rawSlot >= MENU_SIZE) return false;

    if (rawSlot >= 45 && rawSlot <= 53) {
      int newPage = session.currentPage();

      if (rawSlot == 45) {
        if (session.currentPage() > 1) newPage = session.currentPage() - 1;
      } else if (rawSlot == 53) {
        newPage = session.currentPage() + 1;
      }

      if (newPage != session.currentPage()) {
        viewer.closeInventory();
        open(viewer, session.targetName(), session.targetUuid(), newPage);
        return true;
      }
    }

    return false;
  }

  public void removeSession(UUID viewerUuid) {
    activeSessions.remove(viewerUuid);
  }

  public static boolean isHistoryMenu(String title) {
    return title != null && title.startsWith(MENU_PREFIX);
  }

  public static String getTargetNameFromTitle(String title) {
    if (title == null) return null;
    int start = title.indexOf('(');
    int end = title.lastIndexOf(')');
    if (start != -1 && end != -1 && end > start) {
      return ChatColor.stripColor(title.substring(start + 1, end));
    }
    return null;
  }

  private record HistorySession(UUID targetUuid, String targetName, int currentPage) {}
}
