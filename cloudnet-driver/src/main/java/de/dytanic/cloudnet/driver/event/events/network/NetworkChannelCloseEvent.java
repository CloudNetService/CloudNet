package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.network.INetworkChannel;

public final class NetworkChannelCloseEvent extends NetworkEvent {

    private final ChannelType channelType;

    public NetworkChannelCloseEvent(INetworkChannel channel, ChannelType channelType) {
        super(channel);
        this.channelType = channelType;
    }

    public ChannelType getChannelType() {
        return this.channelType;
    }
}