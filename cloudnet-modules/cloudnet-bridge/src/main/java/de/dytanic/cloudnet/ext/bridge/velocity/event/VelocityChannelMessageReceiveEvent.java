package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;

public final class VelocityChannelMessageReceiveEvent extends VelocityCloudNetEvent implements WrappedChannelMessageReceiveEvent {

    private final ChannelMessageReceiveEvent event;

    public VelocityChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
        this.event = event;
    }

    @Override
    public ChannelMessageReceiveEvent getWrapped() {
        return this.event;
    }
}