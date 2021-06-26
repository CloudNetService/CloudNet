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

package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetGroupsEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetUsersEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagementHandler;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Collection;

public final class DefaultPermissionManagementHandler implements IPermissionManagementHandler {

  @Override
  public void handleAddUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionAddUserEvent(permissionManagement, permissionUser));
    this.sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.ADD_USER, permissionUser));
  }

  @Override
  public void handleUpdateUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionUpdateUserEvent(permissionManagement, permissionUser));
    this
      .sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.UPDATE_USER, permissionUser));
  }

  @Override
  public void handleDeleteUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionDeleteUserEvent(permissionManagement, permissionUser));
    this
      .sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.DELETE_USER, permissionUser));
  }

  @Override
  public void handleSetUsers(IPermissionManagement permissionManagement, Collection<? extends IPermissionUser> users) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionSetUsersEvent(permissionManagement, users));
    this.sendAll(PacketServerUpdatePermissions.setUsers(users));
  }

  @Override
  public void handleAddGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionAddGroupEvent(permissionManagement, permissionGroup));
    this
      .sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.ADD_GROUP, permissionGroup));
  }

  @Override
  public void handleUpdateGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionUpdateGroupEvent(permissionManagement, permissionGroup));
    this.sendAll(
      new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.UPDATE_GROUP, permissionGroup));
  }

  @Override
  public void handleDeleteGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionDeleteGroupEvent(permissionManagement, permissionGroup));
    this.sendAll(
      new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.DELETE_GROUP, permissionGroup));
  }

  @Override
  public void handleSetGroups(IPermissionManagement permissionManagement,
    Collection<? extends IPermissionGroup> groups) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new PermissionSetGroupsEvent(permissionManagement, groups));
    this.sendAll(PacketServerUpdatePermissions.setGroups(groups));
  }

  @Override
  public void handleReloaded(IPermissionManagement permissionManagement) {
    this.sendAll(PacketServerUpdatePermissions.setGroups(permissionManagement.getGroups()));
  }

  private void sendAll(IPacket packet) {
    CloudNet.getInstance().sendAll(packet);
  }

}
