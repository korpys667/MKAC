package ru.korpys667.mkac.command.commands;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class HelpCommand implements MKCommand {

  @Override
  public void register(CommandManager<Sender> manager) {
    final var builder = manager.commandBuilder("mkac").permission("mkac.help");

    manager.command(builder.handler(this::help));

    manager.command(builder.literal("help").handler(this::help));
  }

  private void help(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    MessageUtil.sendMessageList(sender.getNativeSender(), Message.HELP_MESSAGE, "command", "mkac");
  }
}
