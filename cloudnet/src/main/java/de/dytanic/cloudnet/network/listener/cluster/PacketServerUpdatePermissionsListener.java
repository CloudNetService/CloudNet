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

package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetGroupsEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetUsersEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.permission.ClusterSynchronizedPermissionManagement;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Collection;

public final class PacketServerUpdatePermissionsListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    packet.getBuffer().markReaderIndex();
    PacketServerUpdatePermissions.UpdateType updateType = packet.getBuffer()
      .readEnumConstant(PacketServerUpdatePermissions.UpdateType.class);

    switch (updateType) {
      case ADD_USER: {
        IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
        this.invoke0(new PermissionAddUserEvent(this.getPermissionManagement(), permissionUser));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.addUserWithoutClusterSyncAsync(permissionUser);
          } else {
            permissionManagement.getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
          }
        }
      }
      break;
      case ADD_GROUP: {
        IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
        this.invoke0(new PermissionAddGroupEvent(this.getPermissionManagement(), permissionGroup));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.addGroupWithoutClusterSyncAsync(permissionGroup);
          } else {
            permissionManagement.getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
          }
        }
      }
      break;
      case SET_USERS: {
        Collection<? extends IPermissionUser> permissionUsers = packet.getBuffer()
          .readObjectCollection(PermissionUser.class);
        this.invoke0(new PermissionSetUsersEvent(this.getPermissionManagement(), permissionUsers));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null && permissionManagement.needsDatabaseSync()) {
          permissionManagement.setUsersWithoutClusterSyncAsync(permissionUsers);
        }
      }
      break;
      case SET_GROUPS: {
        Collection<? extends IPermissionGroup> permissionGroups = packet.getBuffer()
          .readObjectCollection(PermissionGroup.class);
        this.invoke0(new PermissionSetGroupsEvent(this.getPermissionManagement(), permissionGroups));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.setGroupsWithoutClusterSyncAsync(permissionGroups);
          } else {
            permissionManagement.getCachedPermissionGroups().clear();
            permissionGroups
              .forEach(group -> permissionManagement.getCachedPermissionGroups().put(group.getName(), group));
          }
        }
      }
      break;
      case DELETE_USER: {
        IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
        this.invoke0(new PermissionDeleteUserEvent(this.getPermissionManagement(), permissionUser));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.deleteUserWithoutClusterSyncAsync(permissionUser);
          } else {
            permissionManagement.getCachedPermissionUsers().remove(permissionUser.getUniqueId());
          }
        }
      }
      break;
      case UPDATE_USER: {
        IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
        this.invoke0(new PermissionUpdateUserEvent(this.getPermissionManagement(), permissionUser));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.updateUserWithoutClusterSyncAsync(permissionUser);
          } else {
            permissionManagement.getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
          }
        }
      }
      break;
      case DELETE_GROUP: {
        IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
        this.invoke0(new PermissionDeleteGroupEvent(this.getPermissionManagement(), permissionGroup));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.deleteGroupWithoutClusterSyncAsync(permissionGroup);
          } else {
            permissionManagement.getCachedPermissionGroups().remove(permissionGroup.getName());
          }
        }
      }
      break;
      case UPDATE_GROUP: {
        IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
        this.invoke0(new PermissionUpdateGroupEvent(this.getPermissionManagement(), permissionGroup));

        ClusterSynchronizedPermissionManagement permissionManagement = this.getPermissionManagement();
        if (permissionManagement != null) {
          if (permissionManagement.needsDatabaseSync()) {
            permissionManagement.updateGroupWithoutClusterSyncAsync(permissionGroup);
          } else {
            permissionManagement.getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
          }
        }
      }
      break;
      default:
        break;
    }

    packet.getBuffer().resetReaderIndex();
    this.sendUpdateToAllServices(packet);
  }

  private void invoke0(Event event) {
    CloudNetDriver.getInstance().getEventManager().callEvent(event);
  }

  private ClusterSynchronizedPermissionManagement getPermissionManagement() {
    IPermissionManagement management = CloudNetDriver.getInstance().getPermissionManagement();
    return management instanceof ClusterSynchronizedPermissionManagement
      ? (ClusterSynchronizedPermissionManagement) management
      : null;
  }

  private void sendUpdateToAllServices(IPacket packet) {
    for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
      if (cloudService.getNetworkChannel() != null) {
        cloudService.getNetworkChannel().sendPacket(packet);
      }
    }
  }
}
