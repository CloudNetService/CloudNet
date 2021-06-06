package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class NetworkChannelReceiveGroupConfigurationsUpdateEvent extends NetworkEvent implements ICancelable {

  private final NetworkUpdateType updateType;
  private List<GroupConfiguration> groupConfigurations;
  private boolean cancelled;

  public NetworkChannelReceiveGroupConfigurationsUpdateEvent(INetworkChannel channel,
    List<GroupConfiguration> groupConfigurations, NetworkUpdateType updateType) {
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
