package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

public abstract class NetworkEvent extends DriverEvent {

  private final INetworkChannel channel;

  public NetworkEvent(INetworkChannel channel) {
    this.channel = channel;
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }
}
