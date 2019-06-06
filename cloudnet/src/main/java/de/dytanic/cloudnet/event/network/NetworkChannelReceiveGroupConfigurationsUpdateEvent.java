package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

import java.util.List;

public final class NetworkChannelReceiveGroupConfigurationsUpdateEvent extends NetworkEvent implements ICancelable {

    private List<GroupConfiguration> groupConfigurations;

    private boolean cancelled;

    public NetworkChannelReceiveGroupConfigurationsUpdateEvent(INetworkChannel channel, List<GroupConfiguration> groupConfigurations) {
        super(channel);
        this.groupConfigurations = groupConfigurations;
    }

    public List<GroupConfiguration> getGroupConfigurations() {
        return this.groupConfigurations;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setGroupConfigurations(List<GroupConfiguration> groupConfigurations) {
        this.groupConfigurations = groupConfigurations;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String toString() {
        return "NetworkChannelReceiveGroupConfigurationsUpdateEvent(groupConfigurations=" + this.getGroupConfigurations() + ", cancelled=" + this.isCancelled() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkChannelReceiveGroupConfigurationsUpdateEvent))
            return false;
        final NetworkChannelReceiveGroupConfigurationsUpdateEvent other = (NetworkChannelReceiveGroupConfigurationsUpdateEvent) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$groupConfigurations = this.getGroupConfigurations();
        final Object other$groupConfigurations = other.getGroupConfigurations();
        if (this$groupConfigurations == null ? other$groupConfigurations != null : !this$groupConfigurations.equals(other$groupConfigurations))
            return false;
        if (this.isCancelled() != other.isCancelled()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkChannelReceiveGroupConfigurationsUpdateEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $groupConfigurations = this.getGroupConfigurations();
        result = result * PRIME + ($groupConfigurations == null ? 43 : $groupConfigurations.hashCode());
        result = result * PRIME + (this.isCancelled() ? 79 : 97);
        return result;
    }
}