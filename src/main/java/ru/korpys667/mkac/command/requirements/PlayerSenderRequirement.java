package ru.korpys667.mkac.command.requirements;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.command.SenderRequirement;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public final class PlayerSenderRequirement implements SenderRequirement {

  public static final PlayerSenderRequirement PLAYER_SENDER_REQUIREMENT =
      new PlayerSenderRequirement();

  @Override
  public @NonNull Component errorMessage(Sender sender) {
    return MessageUtil.getMessage(Message.RUN_AS_PLAYER);
  }

  @Override
  public boolean evaluateRequirement(@NonNull CommandContext<Sender> commandContext) {
    return commandContext.sender().isPlayer();
  }
}
