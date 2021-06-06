package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.service.ICloudService;

public final class NetworkChannelAuthCloudServiceSuccessEvent extends Event {

  private final ICloudService cloudService;

  private final INetworkChannel channel;

  public NetworkChannelAuthCloudServiceSuccessEvent(ICloudService cloudService, INetworkChannel channel) {
    this.cloudService = cloudService;
    this.channel = channel;
  }

  public ICloudService getCloudService() {
    return this.cloudService;
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }
}
