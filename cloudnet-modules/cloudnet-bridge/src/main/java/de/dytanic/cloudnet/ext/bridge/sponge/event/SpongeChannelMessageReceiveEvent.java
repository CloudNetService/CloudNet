package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;

public final class SpongeChannelMessageReceiveEvent extends SpongeCloudNetEvent implements
  WrappedChannelMessageReceiveEvent {

  private final ChannelMessageReceiveEvent event;

  public SpongeChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
    this.event = event;
  }

  @Override
  public ChannelMessageReceiveEvent getWrapped() {
    return this.event;
  }
}
