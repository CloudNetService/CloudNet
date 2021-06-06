package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;

/**
 * {@inheritDoc}
 */
public final class GoMintChannelMessageReceiveEvent extends GoMintCloudNetEvent implements
  WrappedChannelMessageReceiveEvent {

  private final ChannelMessageReceiveEvent event;

  public GoMintChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
    this.event = event;
  }

  @Override
  public ChannelMessageReceiveEvent getWrapped() {
    return this.event;
  }

}
