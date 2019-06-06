package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.List;

public final class NetworkChannelReceiveServiceTasksUpdateEvent extends NetworkEvent implements ICancelable {

    private List<ServiceTask> serviceTasks;

    private boolean cancelled;

    public NetworkChannelReceiveServiceTasksUpdateEvent(INetworkChannel channel, List<ServiceTask> serviceTasks) {
        super(channel);
        this.serviceTasks = serviceTasks;
    }

    public List<ServiceTask> getServiceTasks() {
        return this.serviceTasks;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setServiceTasks(List<ServiceTask> serviceTasks) {
        this.serviceTasks = serviceTasks;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String toString() {
        return "NetworkChannelReceiveServiceTasksUpdateEvent(serviceTasks=" + this.getServiceTasks() + ", cancelled=" + this.isCancelled() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkChannelReceiveServiceTasksUpdateEvent))
            return false;
        final NetworkChannelReceiveServiceTasksUpdateEvent other = (NetworkChannelReceiveServiceTasksUpdateEvent) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$serviceTasks = this.getServiceTasks();
        final Object other$serviceTasks = other.getServiceTasks();
        if (this$serviceTasks == null ? other$serviceTasks != null : !this$serviceTasks.equals(other$serviceTasks))
            return false;
        if (this.isCancelled() != other.isCancelled()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkChannelReceiveServiceTasksUpdateEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $serviceTasks = this.getServiceTasks();
        result = result * PRIME + ($serviceTasks == null ? 43 : $serviceTasks.hashCode());
        result = result * PRIME + (this.isCancelled() ? 79 : 97);
        return result;
    }
}