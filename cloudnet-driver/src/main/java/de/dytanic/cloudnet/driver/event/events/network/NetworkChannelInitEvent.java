package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class NetworkChannelInitEvent extends NetworkEvent implements ICancelable {

    private final ChannelType channelType;

    @Setter
    private boolean cancelled;

    public NetworkChannelInitEvent(INetworkChannel channel, ChannelType channelType)
    {
        super(channel);
        this.channelType = channelType;
    }
}