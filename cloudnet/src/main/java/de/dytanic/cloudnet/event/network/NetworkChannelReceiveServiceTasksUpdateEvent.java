package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode(callSuper = false)
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

}