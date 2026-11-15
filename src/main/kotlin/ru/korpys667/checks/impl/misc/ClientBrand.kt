/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.checks.impl.misc

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage
import java.nio.charset.StandardCharsets
import net.kyori.adventure.text.Component
import ru.korpys667.alert.AlertManager
import ru.korpys667.alert.AlertType
import ru.korpys667.checks.AbstractCheck
import ru.korpys667.checks.CheckData
import ru.korpys667.checks.CheckFactory
import ru.korpys667.checks.type.PacketCheck
import ru.korpys667.config.ConfigManager
import ru.korpys667.player.MKACPlayer
import ru.korpys667.utils.ChatUtil
import ru.korpys667.utils.Message
import ru.korpys667.utils.MessageUtil

@CheckData(name = "ClientBrand_Internal")
class ClientBrand(
  mkacPlayer: MKACPlayer,
  private val configManager: ConfigManager,
  private val alertManager: AlertManager,
) : AbstractCheck(mkacPlayer), PacketCheck {
  companion object {
    private val CHANNEL =
      if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_13)) {
        "minecraft:brand"
      } else {
        "MC|Brand"
      }
  }

  var brand: String = "vanilla"
    private set

  private var hasBrand = false

  interface Factory : CheckFactory {
    override fun create(player: MKACPlayer): ClientBrand
  }

  override fun onPacketReceive(event: PacketReceiveEvent) {
    if (event.packetType == PacketType.Play.Client.PLUGIN_MESSAGE) {
      val packet = WrapperPlayClientPluginMessage(event)
      handle(packet.channelName, packet.data)
    } else if (event.packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
      val packet = WrapperConfigClientPluginMessage(event)
      handle(packet.channelName, packet.data)
    }
  }

  private fun handle(channel: String, data: ByteArray) {
    if (channel != CHANNEL || hasBrand) {
      return
    }

    val mkacPlayer = mkacPlayer
    hasBrand = true

    if (data.size > 64 || data.isEmpty()) {
      brand = "invalid (${data.size} bytes)"
    } else {
      val brandBytes = ByteArray(data.size - 1)
      System.arraycopy(data, 1, brandBytes, 0, brandBytes.size)

      brand = String(brandBytes, StandardCharsets.UTF_8).replace(" (Velocity)", "")
      brand = ChatUtil.stripColor(brand) ?: brand
    }

    mkacPlayer.brand = brand

    if (!configManager.isClientIgnored(brand)) {
      val component: Component =
        MessageUtil.getMessage(
          Message.BRAND_NOTIFICATION,
          "player",
          mkacPlayer.name,
          "brand",
          brand,
        )
      alertManager.send(component, AlertType.BRAND)
    }

    val hasReachExploit =
      brand.contains("forge") &&
        mkacPlayer.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_18_2) &&
        mkacPlayer.user.clientVersion.isOlderThan(ClientVersion.V_1_19_4)

    if (hasReachExploit && configManager.isDisconnectBlacklistedForge()) {
      mkacPlayer.disconnect(MessageUtil.getMessage(Message.BRAND_DISCONNECT_FORGE))
    }
  }
}
