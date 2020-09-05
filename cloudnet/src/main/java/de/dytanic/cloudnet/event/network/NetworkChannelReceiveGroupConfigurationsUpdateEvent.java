package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class NetworkChannelReceiveGroupConfigurationsUpdateEvent extends NetworkEvent implements ICancelable {

    private List<GroupConfiguration> groupConfigurations;
    private final NetworkUpdateType updateType;

    private boolean cancelled;

    public NetworkChannelReceiveGroupConfigurationsUpdateEvent(INetworkChannel channel, List<GroupConfiguration> groupConfigurations, NetworkUpdateType updateType) {
        super(channel);
        this.groupConfigurations = groupConfigurations;
        this.updateType = updateType;
    }

    public NetworkUpdateType getUpdateType() {
        return this.updateType;
    }

    public List<GroupConfiguration> getGroupConfigurations() {
        return this.groupConfigurations;
    }

    public void setGroupConfigurations(List<GroupConfiguration> groupConfigurations) {
        this.groupConfigurations = groupConfigurations;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}