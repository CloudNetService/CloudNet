/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class NetworkChannelReceiveJsonFilePermissionsUpdateEvent extends NetworkEvent implements ICancelable {

  private List<PermissionUser> permissionUsers;

  private List<PermissionGroup> permissionGroups;

  private boolean cancelled;

  public NetworkChannelReceiveJsonFilePermissionsUpdateEvent(INetworkChannel channel,
    List<PermissionUser> permissionUsers, List<PermissionGroup> permissionGroups) {
    super(channel);
    this.permissionUsers = permissionUsers;
    this.permissionGroups = permissionGroups;
  }

  public List<PermissionUser> getPermissionUsers() {
    return this.permissionUsers;
  }

  public void setPermissionUsers(List<PermissionUser> permissionUsers) {
    this.permissionUsers = permissionUsers;
  }

  public List<PermissionGroup> getPermissionGroups() {
    return this.permissionGroups;
  }

  public void setPermissionGroups(List<PermissionGroup> permissionGroups) {
    this.permissionGroups = permissionGroups;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

}
