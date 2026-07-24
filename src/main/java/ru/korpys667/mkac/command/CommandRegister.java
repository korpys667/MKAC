package ru.korpys667.mkac.command;

import io.leangen.geantyref.TypeToken;
import java.util.function.Function;
import net.md_5.bungee.api.ChatColor;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.command.commands.*;
import ru.korpys667.mkac.command.handler.MKCommandFailureHandler;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.config.LocaleManager;
import ru.korpys667.mkac.database.DatabaseManager;
import ru.korpys667.mkac.menu.HistoryMenu;
import ru.korpys667.mkac.player.PlayerDataManager;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.MessageUtil;

public class CommandRegister {

  public static final CloudKey<Requirements<Sender, SenderRequirement>> REQUIREMENT_KEY =
      CloudKey.of("mkac_requirements", new TypeToken<>() {});

  public static final RequirementApplicable.RequirementApplicableFactory<Sender, SenderRequirement>
      REQUIREMENT_FACTORY = RequirementApplicable.factory(REQUIREMENT_KEY);

  private static boolean commandsRegistered = false;

  public static void registerCommands(
      org.incendo.cloud.CommandManager<Sender> commandManager,
      MKAC plugin,
      AlertManager alertManager,
      DatabaseManager databaseManager,
      ConfigManager configManager,
      LocaleManager localeManager,
      PlayerDataManager playerDataManager,
      HistoryMenu historyMenu) {

    if (commandsRegistered) return;

    new HelpCommand().register(commandManager);
    new AlertsCommand(alertManager).register(commandManager);
    new ReloadCommand(plugin).register(commandManager);
    new ProbCommand(playerDataManager, localeManager, plugin).register(commandManager);
    new ProfileCommand(playerDataManager, localeManager).register(commandManager);
    new HistoryCommand(historyMenu).register(commandManager);
    new LogsCommand(plugin, databaseManager, configManager, localeManager).register(commandManager);
    new PunishCommand(databaseManager).register(commandManager);
    new BrandsCommand(alertManager).register(commandManager);
    new SuspiciousCommand(playerDataManager, alertManager).register(commandManager);
    new StatsCommand(plugin, databaseManager, playerDataManager).register(commandManager);
    new MenuCommand(plugin.getChickenCoopMenu()).register(commandManager);
    new FalsePositiveCommand(plugin, playerDataManager).register(commandManager);
    new StatusCommand(plugin.getHologramManager()).register(commandManager);

    final RequirementPostprocessor<Sender, SenderRequirement> senderRequirementPostprocessor =
        RequirementPostprocessor.of(REQUIREMENT_KEY, new MKCommandFailureHandler());
    commandManager.registerCommandPostProcessor(senderRequirementPostprocessor);

    registerExceptionHandler(
        commandManager, InvalidSyntaxException.class, e -> MessageUtil.format(e.correctSyntax()));

    commandsRegistered = true;
  }

  private static <E extends Exception> void registerExceptionHandler(
      org.incendo.cloud.CommandManager<Sender> commandManager,
      Class<E> ex,
      Function<E, String> toMessage) {
    commandManager
        .exceptionController()
        .registerHandler(
            ex,
            (c) ->
                c.context().sender().sendMessage(ChatColor.RED + toMessage.apply(c.exception())));
  }
}
