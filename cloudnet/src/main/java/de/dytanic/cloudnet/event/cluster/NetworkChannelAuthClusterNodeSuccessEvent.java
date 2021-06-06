package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

public final class NetworkChannelAuthClusterNodeSuccessEvent extends Event {

  private final IClusterNodeServer node;

  private final INetworkChannel channel;

  public NetworkChannelAuthClusterNodeSuccessEvent(IClusterNodeServer node, INetworkChannel channel) {
    this.node = node;
    this.channel = channel;
  }

  public IClusterNodeServer getNode() {
    return this.node;
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }
}
