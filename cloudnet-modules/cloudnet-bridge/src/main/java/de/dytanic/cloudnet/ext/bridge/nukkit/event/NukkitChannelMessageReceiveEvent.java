package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;

/**
 * {@inheritDoc}
 */
public final class NukkitChannelMessageReceiveEvent extends NukkitCloudNetEvent implements
  WrappedChannelMessageReceiveEvent {

  private static final HandlerList handlers = new HandlerList();

  private final ChannelMessageReceiveEvent event;

  public NukkitChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
    this.event = event;
  }

  public static HandlerList getHandlers() {
    return NukkitChannelMessageReceiveEvent.handlers;
  }

  @Override
  public ChannelMessageReceiveEvent getWrapped() {
    return this.event;
  }
}
