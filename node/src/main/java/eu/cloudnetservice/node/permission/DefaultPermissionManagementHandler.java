/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.permission;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.permission.PermissionAddGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionAddUserEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionDeleteGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionDeleteUserEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionSetGroupsEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateUserEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.permission.handler.PermissionManagementHandler;
import java.util.Collection;
import lombok.NonNull;

public final class DefaultPermissionManagementHandler implements PermissionManagementHandler {

  private final EventManager eventManager;

  public DefaultPermissionManagementHandler(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public void handleAddUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
    this.eventManager.callEvent(new PermissionAddUserEvent(management, user));
    this.baseMessage("add_user").buffer(DataBuf.empty().writeObject(user)).build().send();
  }

  @Override
  public void handleUpdateUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
    this.eventManager.callEvent(new PermissionUpdateUserEvent(management, user));
    this.baseMessage("update_user").buffer(DataBuf.empty().writeObject(user)).build().send();
  }

  @Override
  public void handleDeleteUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
    this.eventManager.callEvent(new PermissionDeleteUserEvent(management, user));
    this.baseMessage("delete_user").buffer(DataBuf.empty().writeObject(user)).build().send();
  }

  @Override
  public void handleAddGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
    this.eventManager.callEvent(new PermissionAddGroupEvent(management, group));
    this.baseMessage("add_group").buffer(DataBuf.empty().writeObject(group)).build().send();
  }

  @Override
  public void handleUpdateGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
    this.eventManager.callEvent(new PermissionUpdateGroupEvent(management, group));
    this.baseMessage("update_group").buffer(DataBuf.empty().writeObject(group)).build().send();
  }

  @Override
  public void handleDeleteGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
    this.eventManager.callEvent(new PermissionDeleteGroupEvent(management, group));
    this.baseMessage("delete_group").buffer(DataBuf.empty().writeObject(group)).build().send();
  }

  @Override
  public void handleSetGroups(@NonNull PermissionManagement management, @NonNull Collection<PermissionGroup> groups) {
    this.eventManager.callEvent(new PermissionSetGroupsEvent(management, groups));
    this.baseMessage("set_groups").buffer(DataBuf.empty().writeObject(groups)).build().send();
  }

  @Override
  public void handleReloaded(@NonNull PermissionManagement management) {
    this.handleSetGroups(management, management.groups());
  }

  private @NonNull ChannelMessage.Builder baseMessage(@NonNull String subMessage) {
    return ChannelMessage.builder()
      .targetAll()
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .message("permissions_" + subMessage);
  }
}
