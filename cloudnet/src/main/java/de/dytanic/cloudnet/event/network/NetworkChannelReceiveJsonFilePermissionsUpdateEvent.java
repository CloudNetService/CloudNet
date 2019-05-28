package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class NetworkChannelReceiveJsonFilePermissionsUpdateEvent extends
    NetworkEvent implements ICancelable {

  private List<PermissionUser> permissionUsers;

  private List<PermissionGroup> permissionGroups;

  private boolean cancelled;

  public NetworkChannelReceiveJsonFilePermissionsUpdateEvent(
      INetworkChannel channel, List<PermissionUser> permissionUsers,
      List<PermissionGroup> permissionGroups) {
    super(channel);
    this.permissionUsers = permissionUsers;
    this.permissionGroups = permissionGroups;
  }
}