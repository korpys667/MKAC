package ru.korpys667.mkac.redis;

import ru.korpys667.mkac.alert.AlertType;

@FunctionalInterface
public interface CrossServerPublisher {
  void publish(AlertType type, String message);
}
