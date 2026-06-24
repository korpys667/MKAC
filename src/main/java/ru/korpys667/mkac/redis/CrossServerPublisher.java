package ru.korpys667.mkac.redis;

import net.kyori.adventure.text.Component;
import ru.korpys667.mkac.alert.AlertType;

@FunctionalInterface
public interface CrossServerPublisher {
  void publish(AlertType type, Component component);
}
