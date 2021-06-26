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

package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.Collection;

public class DriverPermissionManagementListener extends CategorizedDriverAPIListener {

  public DriverPermissionManagementListener() {
    super(DriverAPICategory.PERMISSION_MANAGEMENT);

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_RELOAD, (channel, packet, buffer) -> {
      boolean success = this.permissionManagement().reload();
      return ProtocolBuffer.create().writeBoolean(success);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_ADD_USER, (channel, packet, buffer) -> {
      IPermissionUser user = buffer.readObject(PermissionUser.class);
      IPermissionUser response = this.permissionManagement().addUser(user);
      return ProtocolBuffer.create().writeObject(response);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_UPDATE_USER, (channel, packet, buffer) -> {
      IPermissionUser user = buffer.readObject(PermissionUser.class);
      this.permissionManagement().updateUser(user);
      return ProtocolBuffer.EMPTY;
    });

    super
      .registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_USERS_BY_NAME, (channel, packet, buffer) -> {
        String name = buffer.readString();
        boolean success = this.permissionManagement().deleteUser(name);
        return ProtocolBuffer.create().writeBoolean(success);
      });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_USER, (channel, packet, buffer) -> {
      IPermissionUser user = buffer.readObject(PermissionUser.class);
      boolean success = this.permissionManagement().deleteUser(user);
      return ProtocolBuffer.create().writeBoolean(success);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_SET_USERS, (channel, packet, buffer) -> {
      Collection<? extends IPermissionUser> users = buffer.readObjectCollection(PermissionUser.class);
      this.permissionManagement().setUsers(users);
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_ADD_GROUP, (channel, packet, buffer) -> {
      IPermissionGroup group = buffer.readObject(PermissionGroup.class);
      IPermissionGroup response = this.permissionManagement().addGroup(group);
      return ProtocolBuffer.create().writeObject(response);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_UPDATE_GROUP, (channel, packet, buffer) -> {
      IPermissionGroup group = buffer.readObject(PermissionGroup.class);
      this.permissionManagement().updateGroup(group);
      return ProtocolBuffer.EMPTY;
    });

    super
      .registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_GROUP_BY_NAME, (channel, packet, buffer) -> {
        String name = buffer.readString();
        this.permissionManagement().deleteGroup(name);
        return ProtocolBuffer.EMPTY;
      });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_DELETE_GROUP, (channel, packet, buffer) -> {
      IPermissionGroup group = buffer.readObject(PermissionGroup.class);
      this.permissionManagement().deleteGroup(group);
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_SET_GROUPS, (channel, packet, buffer) -> {
      Collection<? extends IPermissionGroup> groups = buffer.readObjectCollection(PermissionGroup.class);
      this.permissionManagement().setGroups(groups);
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_CONTAINS_USER_BY_UNIQUE_ID,
      (channel, packet, buffer) -> {
        boolean contains = this.permissionManagement().containsUser(buffer.readUUID());
        return ProtocolBuffer.create().writeBoolean(contains);
      });

    super
      .registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_CONTAINS_USER_BY_NAME, (channel, packet, buffer) -> {
        boolean contains = this.permissionManagement().containsUser(buffer.readString());
        return ProtocolBuffer.create().writeBoolean(contains);
      });

    super
      .registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USER_BY_UNIQUE_ID, (channel, packet, buffer) -> {
        IPermissionUser user = this.permissionManagement().getUser(buffer.readUUID());
        return ProtocolBuffer.create().writeOptionalObject(user);
      });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_OR_CREATE_USER, (channel, packet, buffer) -> {
      IPermissionUser user = this.permissionManagement().getOrCreateUser(buffer.readUUID(), buffer.readString());
      return ProtocolBuffer.create().writeObject(user);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS_BY_NAME, (channel, packet, buffer) -> {
      Collection<IPermissionUser> users = this.permissionManagement().getUsers(buffer.readString());
      return ProtocolBuffer.create().writeObjectCollection(users);
    });

    super
      .registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_FIRST_USER_BY_NAME, (channel, packet, buffer) -> {
        IPermissionUser user = this.permissionManagement().getFirstUser(buffer.readString());
        return ProtocolBuffer.create().writeOptionalObject(user);
      });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS, (channel, packet, buffer) -> {
      Collection<IPermissionUser> users = this.permissionManagement().getUsers();
      return ProtocolBuffer.create().writeObjectCollection(users);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_USERS_BY_GROUP, (channel, packet, buffer) -> {
      Collection<IPermissionUser> users = this.permissionManagement().getUsersByGroup(buffer.readString());
      return ProtocolBuffer.create().writeObjectCollection(users);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_CONTAINS_GROUP, (channel, packet, buffer) -> {
      boolean contains = this.permissionManagement().containsGroup(buffer.readString());
      return ProtocolBuffer.create().writeBoolean(contains);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_GROUP_BY_NAME, (channel, packet, buffer) -> {
      IPermissionGroup group = this.permissionManagement().getGroup(buffer.readString());
      return ProtocolBuffer.create().writeOptionalObject(group);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_GROUPS, (channel, packet, buffer) -> {
      Collection<IPermissionGroup> groups = this.permissionManagement().getGroups();
      return ProtocolBuffer.create().writeObjectCollection(groups);
    });

    super.registerHandler(DriverAPIRequestType.PERMISSION_MANAGEMENT_GET_DEFAULT_GROUP, (channel, packet, buffer) -> {
      IPermissionGroup group = this.permissionManagement().getDefaultPermissionGroup();
      return ProtocolBuffer.create().writeOptionalObject(group);
    });

  }

  private IPermissionManagement permissionManagement() {
    return CloudNetDriver.getInstance().getPermissionManagement();
  }

}
