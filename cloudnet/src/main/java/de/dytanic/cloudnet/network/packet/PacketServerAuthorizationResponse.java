package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketServerAuthorizationResponse extends Packet {

  public PacketServerAuthorizationResponse(boolean access, String text) {
    super(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL,
      new JsonDocument("access", access).append("text", text),
      Packet.EMPTY_PACKET_BYTE_ARRAY);
  }
}