package ru.korpys667.mkac.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import ru.korpys667.mkac.config.LocaleManager;

public class MessageUtil {

  private static LocaleManager localeManager;
  private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  public static void init(LocaleManager localeManager) {
    MessageUtil.localeManager = localeManager;
  }

  public static String colorize(String message) {
    if (message == null) return null;
    Matcher matcher = HEX_PATTERN.matcher(message);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String color = "#" + matcher.group(1);
      matcher.appendReplacement(buffer, ChatColor.of(color).toString());
    }
    return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
  }

  public static String format(String message, String... placeholders) {
    String processedMessage =
        message.replace("<prefix>", localeManager.getRawMessage(Message.PREFIX));

    if (placeholders.length > 0) {
      if (placeholders.length % 2 != 0) {
        System.err.println("Invalid placeholders count for message: " + message);
      } else {
        for (int i = 0; i < placeholders.length; i += 2) {
          String key = placeholders[i];
          String value = placeholders[i + 1];
          processedMessage = processedMessage.replace("<" + key + ">", value);
        }
      }
    }

    return colorize(processedMessage);
  }

  public static void sendMessage(CommandSender sender, Message key, String... placeholders) {
    sender.sendMessage(getMessage(key, placeholders));
  }

  public static void sendMessageList(CommandSender sender, Message key, String... placeholders) {
    getMessageList(key, placeholders).forEach(sender::sendMessage);
  }

  public static String getMessage(Message key, String... placeholders) {
    String rawMessage = localeManager.getRawMessage(key);
    return format(rawMessage, placeholders);
  }

  public static List<String> getMessageList(Message key, String... placeholders) {
    return localeManager.getRawMessageList(key).stream()
        .map(line -> format(line, placeholders))
        .collect(Collectors.toList());
  }
}
