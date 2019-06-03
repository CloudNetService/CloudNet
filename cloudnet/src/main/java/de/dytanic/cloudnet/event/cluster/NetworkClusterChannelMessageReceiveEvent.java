package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import lombok.Getter;

@Getter
public class NetworkClusterChannelMessageReceiveEvent extends NetworkEvent {

  private final String messageChannel, message;

  private final JsonDocument header;

  private final byte[] body;

  public NetworkClusterChannelMessageReceiveEvent(
      INetworkChannel networkChannel,
      String channel, String message, JsonDocument header, byte[] body) {
    super(networkChannel);

    this.messageChannel = channel;
    this.message = message;
    this.header = header;
    this.body = body;
  }
}