package ru.korpys667.mkac.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.processors.requirements.Requirement;
import ru.korpys667.mkac.sender.Sender;

public interface SenderRequirement extends Requirement<Sender, SenderRequirement> {
  @NonNull String errorMessage(Sender sender);
}
