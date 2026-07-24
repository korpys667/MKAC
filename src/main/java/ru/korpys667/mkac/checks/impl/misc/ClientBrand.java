package ru.korpys667.mkac.checks.impl.misc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;
import ru.korpys667.mkac.alert.AlertManager;
import ru.korpys667.mkac.alert.AlertType;
import ru.korpys667.mkac.checks.AbstractCheck;
import ru.korpys667.mkac.checks.CheckData;
import ru.korpys667.mkac.checks.type.PacketCheck;
import ru.korpys667.mkac.config.ConfigManager;
import ru.korpys667.mkac.player.MKPlayer;
import ru.korpys667.mkac.utils.ChatUtil;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

@CheckData(name = "ClientBrand_Internal")
public class ClientBrand extends AbstractCheck implements PacketCheck {

  private static final String CHANNEL =
      PacketEvents.getAPI()
              .getServerManager()
              .getVersion()
              .isNewerThanOrEquals(ServerVersion.V_1_13)
          ? "minecraft:brand"
          : "MC|Brand";

  private final ConfigManager configManager;
  private final AlertManager alertManager;

  @Getter private String brand = "vanilla";
  private boolean hasBrand = false;

  public ClientBrand(MKPlayer player, ConfigManager configManager, AlertManager alertManager) {
    super(player);
    this.configManager = configManager;
    this.alertManager = alertManager;
  }

  @Override
  public void onPacketReceive(final PacketReceiveEvent event) {
    if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
      WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
      handle(packet.getChannelName(), packet.getData());
    } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
      WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
      handle(packet.getChannelName(), packet.getData());
    }
  }

  private void handle(String channel, byte[] data) {
    if (!channel.equals(ClientBrand.CHANNEL) || hasBrand) {
      return;
    }

    hasBrand = true;

    if (data.length > 64 || data.length == 0) {
      brand = "invalid (" + data.length + " bytes)";
    } else {
      byte[] brandBytes = new byte[data.length - 1];
      System.arraycopy(data, 1, brandBytes, 0, brandBytes.length);

      brand = new String(brandBytes).replace(" (Velocity)", "");
      brand = ChatUtil.stripColor(brand);
    }

    mkPlayer.setBrand(brand);

    if (!configManager.isClientIgnored(brand)) {
      String message =
          MessageUtil.getMessage(
              Message.BRAND_NOTIFICATION, "player", mkPlayer.getPlayer().getName(), "brand", brand);
      alertManager.send(message, AlertType.BRAND);
    }

    final boolean hasReachExploit =
        brand.contains("forge")
            && mkPlayer.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)
            && mkPlayer.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_19_4);

    if (hasReachExploit && configManager.isDisconnectBlacklistedForge()) {
      mkPlayer.disconnect(MessageUtil.getMessage(Message.BRAND_DISCONNECT_FORGE));
    }
  }
}
