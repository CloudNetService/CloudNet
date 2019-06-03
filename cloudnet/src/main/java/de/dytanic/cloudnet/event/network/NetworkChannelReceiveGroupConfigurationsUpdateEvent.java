package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public final class NetworkChannelReceiveGroupConfigurationsUpdateEvent extends NetworkEvent implements ICancelable {

    private List<GroupConfiguration> groupConfigurations;

    private boolean cancelled;

    public NetworkChannelReceiveGroupConfigurationsUpdateEvent(INetworkChannel channel, List<GroupConfiguration> groupConfigurations) {
        super(channel);
        this.groupConfigurations = groupConfigurations;
    }
}