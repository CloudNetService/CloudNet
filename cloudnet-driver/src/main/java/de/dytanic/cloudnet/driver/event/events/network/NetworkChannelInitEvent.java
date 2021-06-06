package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

public class NetworkChannelInitEvent extends NetworkEvent implements ICancelable {

  private final ChannelType channelType;

  private boolean cancelled;

  public NetworkChannelInitEvent(INetworkChannel channel, ChannelType channelType) {
    super(channel);
    this.channelType = channelType;
  }

  public ChannelType getChannelType() {
    return this.channelType;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
