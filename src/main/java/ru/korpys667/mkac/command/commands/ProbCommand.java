package ru.korpys667.mkac.command.commands;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.checks.impl.ai.AICheck;
import ru.korpys667.mkac.command.CommandRegister;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.command.requirements.PlayerSenderRequirement;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class ProbCommand implements MKCommand, Listener {
  private final Map<UUID, ProbSession> activeSessions = new ConcurrentHashMap<>();

  private final PlayerDataManager playerDataManager;
  private final LocaleManager localeManager;
  private final MKAC plugin;

  public ProbCommand(
      PlayerDataManager playerDataManager, LocaleManager localeManager, MKAC plugin) {
    this.playerDataManager = playerDataManager;
    this.localeManager = localeManager;
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("prob")
            .permission("mkac.prob")
            .required("target", PlayerParser.playerParser())
            .apply(
                CommandRegister.REQUIREMENT_FACTORY.create(
                    PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
            .handler(this::execute));
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    final UUID uuid = player.getUniqueId();

    if (activeSessions.containsKey(uuid)) {
      stop(player);
    }

    UUID viewerUuid = null;
    for (Map.Entry<UUID, ProbSession> entry : activeSessions.entrySet()) {
      if (entry.getValue().targetUuid().equals(uuid)) {
        viewerUuid = entry.getKey();
        break;
      }
    }

    if (viewerUuid != null) {
      Player viewer = Bukkit.getPlayer(viewerUuid);
      if (viewer != null) {
        stop(viewer);
        MessageUtil.sendMessage(viewer, Message.PROB_DISABLED, "player", player.getName());
      } else {
        activeSessions.remove(viewerUuid);
      }
    }
  }

  private void execute(CommandContext<Sender> context) {
    final Player player = context.sender().getPlayer();
    final Player target = context.get("target");

    final ProbSession session = activeSessions.get(player.getUniqueId());

    if (session != null && session.targetUuid().equals(target.getUniqueId())) {
      stop(player);
      MessageUtil.sendMessage(player, Message.PROB_DISABLED, "player", target.getName());
      return;
    }

    if (session != null) {
      stop(player);
    }

    start(player, target);
    MessageUtil.sendMessage(player, Message.PROB_ENABLED, "player", target.getName());
  }

  private void start(Player viewer, Player target) {
    final UUID viewerId = viewer.getUniqueId();
    final UUID targetId = target.getUniqueId();

    final ActionBarStrings strings = new ActionBarStrings(localeManager);

    final BukkitTask task =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  final Player onlineViewer = Bukkit.getPlayer(viewerId);
                  final Player onlineTarget = Bukkit.getPlayer(targetId);

                  if (onlineViewer == null
                      || !onlineViewer.isOnline()
                      || onlineTarget == null
                      || !onlineTarget.isOnline()) {
                    if (onlineViewer != null) stop(onlineViewer);
                    return;
                  }

                  final MKPlayer mkTarget = playerDataManager.getPlayer(onlineTarget);
                  if (mkTarget == null) {
                    sendActionBar(
                        onlineViewer,
                        MessageUtil.getMessage(
                            Message.PROB_NO_DATA, "player", onlineTarget.getName()));
                    return;
                  }

                  final AICheck aiCheck = mkTarget.getCheckManager().getCheck(AICheck.class);
                  if (aiCheck == null) {
                    sendActionBar(
                        onlineViewer,
                        MessageUtil.getMessage(
                            Message.PROB_NO_AICHECK, "player", onlineTarget.getName()));
                    return;
                  }

                  sendActionBar(onlineViewer, buildActionBar(aiCheck, onlineTarget, strings));
                },
                0L,
                2L);

    final ProbSession newSession = new ProbSession(targetId, task, strings);
    activeSessions.put(viewerId, newSession);
  }

  private void stop(Player viewer) {
    final ProbSession session = activeSessions.remove(viewer.getUniqueId());
    if (session != null) {
      session.task().cancel();
      sendActionBar(viewer, "");
    }
  }

  private String buildActionBar(AICheck aiCheck, Player target, ActionBarStrings strings) {
    final double probability = aiCheck.getLastProbability();
    final double violationLevel = aiCheck.getBuffer();
    final int ping = target.getPing();

    final ChatColor probColor = getProbColor(probability);
    final ChatColor vlColor = getVlColor(violationLevel);
    final ChatColor pingColor = getPingColor(ping);

    String bufferStr = String.format(Locale.US, "%.2f", violationLevel);
    if (violationLevel > 30) {
      bufferStr = ChatColor.BOLD + bufferStr;
    }

    return probColor
        + strings.labelProb()
        + " ("
        + target.getName()
        + "): "
        + String.format(Locale.US, "%.4f", probability)
        + ChatColor.DARK_GRAY
        + strings.separator()
        + vlColor
        + strings.labelBuffer()
        + ": "
        + bufferStr
        + ChatColor.DARK_GRAY
        + strings.separator()
        + pingColor
        + strings.labelPing()
        + ": "
        + ping
        + strings.suffixPing();
  }

  private void sendActionBar(Player player, String message) {
    if (player == null || !player.isOnline()) return;
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
  }

  private ChatColor getProbColor(double probability) {
    if (probability > 0.9) return ChatColor.RED;
    if (probability > 0.5) return ChatColor.YELLOW;
    return ChatColor.GREEN;
  }

  private ChatColor getVlColor(double violationLevel) {
    if (violationLevel > 30) return ChatColor.DARK_RED;
    if (violationLevel > 15) return ChatColor.RED;
    return ChatColor.GREEN;
  }

  private ChatColor getPingColor(int ping) {
    if (ping > 150) return ChatColor.RED;
    if (ping > 80) return ChatColor.YELLOW;
    return ChatColor.GREEN;
  }

  private record ProbSession(UUID targetUuid, BukkitTask task, ActionBarStrings strings) {}

  private record ActionBarStrings(
      String labelProb, String labelBuffer, String labelPing, String separator, String suffixPing) {
    ActionBarStrings(LocaleManager lm) {
      this(
          MessageUtil.colorize(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PROB)),
          MessageUtil.colorize(lm.getRawMessage(Message.PROB_FORMAT_LABEL_BUFFER)),
          MessageUtil.colorize(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PING)),
          MessageUtil.colorize(lm.getRawMessage(Message.PROB_FORMAT_SEPARATOR)),
          MessageUtil.colorize(lm.getRawMessage(Message.PROB_FORMAT_SUFFIX_PING)));
    }
  }
}
