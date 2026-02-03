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
package ru.korpys667.mkac.command.commands;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    final ActionBarComponents components = new ActionBarComponents(localeManager);

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

                  sendActionBar(onlineViewer, buildActionBar(aiCheck, onlineTarget, components));
                },
                0L,
                2L);

    final ProbSession newSession = new ProbSession(targetId, task, components);
    activeSessions.put(viewerId, newSession);
  }

  private void stop(Player viewer) {
    final ProbSession session = activeSessions.remove(viewer.getUniqueId());
    if (session != null) {
      session.task().cancel();
      sendActionBar(viewer, Component.empty());
    }
  }

  private Component buildActionBar(AICheck aiCheck, Player target, ActionBarComponents components) {
    final double probability = aiCheck.getLastProbability();
    final double violationLevel = aiCheck.getBuffer();
    final int ping = target.getPing();

    final TextColor probColor = getProbColor(probability);
    final TextColor vlColor = getVlColor(violationLevel);
    final TextColor pingColor = getPingColor(ping);

    TextComponent bufferComponent =
        Component.text(String.format(Locale.US, "%.2f", violationLevel), vlColor);
    if (violationLevel > 30) {
      bufferComponent = bufferComponent.decorate(TextDecoration.BOLD);
    }

    return Component.text()
        .append(components.labelProb().color(probColor))
        .append(components.openParen().color(probColor))
        .append(Component.text(target.getName(), probColor))
        .append(components.closeParen().color(probColor))
        .append(Component.text(String.format(Locale.US, "%.4f", probability), probColor))
        .append(components.separator())
        .append(components.labelBuffer().color(vlColor))
        .append(components.colon().color(vlColor))
        .append(bufferComponent)
        .append(components.separator())
        .append(components.labelPing().color(pingColor))
        .append(components.colon().color(pingColor))
        .append(Component.text(ping, pingColor))
        .append(components.suffixPing().color(pingColor))
        .build();
  }

  private void sendActionBar(Player player, Component message) {
    if (player == null || !player.isOnline()) return;
    plugin.getAdventure().player(player).sendActionBar(message);
  }

  private TextColor getProbColor(double probability) {
    if (probability > 0.9) return NamedTextColor.RED;
    if (probability > 0.5) return NamedTextColor.YELLOW;
    return NamedTextColor.GREEN;
  }

  private TextColor getVlColor(double violationLevel) {
    if (violationLevel > 30) return NamedTextColor.DARK_RED;
    if (violationLevel > 15) return NamedTextColor.RED;
    return NamedTextColor.GREEN;
  }

  private TextColor getPingColor(int ping) {
    if (ping > 150) return NamedTextColor.RED;
    if (ping > 80) return NamedTextColor.YELLOW;
    return NamedTextColor.GREEN;
  }

  private record ProbSession(UUID targetUuid, BukkitTask task, ActionBarComponents components) {}

  private record ActionBarComponents(
      Component labelProb,
      Component labelBuffer,
      Component labelPing,
      Component separator,
      Component suffixPing,
      Component openParen,
      Component closeParen,
      Component colon) {
    ActionBarComponents(LocaleManager lm) {
      this(
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PROB)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_BUFFER)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PING)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_SEPARATOR), NamedTextColor.DARK_GRAY),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_SUFFIX_PING)),
          Component.text(" ("),
          Component.text("): "),
          Component.text(": "));
    }
  }
}
