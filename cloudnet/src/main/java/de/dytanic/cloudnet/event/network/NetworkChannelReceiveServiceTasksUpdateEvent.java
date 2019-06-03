package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class NetworkChannelReceiveServiceTasksUpdateEvent extends
    NetworkEvent implements ICancelable {

  private List<ServiceTask> serviceTasks;

  private boolean cancelled;

  public NetworkChannelReceiveServiceTasksUpdateEvent(INetworkChannel channel,
      List<ServiceTask> serviceTasks) {
    super(channel);
    this.serviceTasks = serviceTasks;
  }
}