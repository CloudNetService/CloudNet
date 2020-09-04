package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;

public final class BungeeChannelMessageReceiveEvent extends BungeeCloudNetEvent implements WrappedChannelMessageReceiveEvent {

    private final ChannelMessageReceiveEvent event;

    public BungeeChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
        this.event = event;
    }

    @Override
    public ChannelMessageReceiveEvent getWrapped() {
        return this.event;
    }
}