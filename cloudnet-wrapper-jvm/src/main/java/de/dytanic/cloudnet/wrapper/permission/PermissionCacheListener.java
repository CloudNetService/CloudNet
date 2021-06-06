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

package de.dytanic.cloudnet.wrapper.permission;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetGroupsEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PermissionCacheListener {

  private final WrapperPermissionManagement permissionManagement;

  public PermissionCacheListener(WrapperPermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @EventListener
  public void handle(PermissionUpdateUserEvent event) {
    IPermissionUser user = event.getPermissionUser();
    if (this.permissionManagement.getCachedPermissionUsers().containsKey(user.getUniqueId())) {
      this.permissionManagement.getCachedPermissionUsers().put(user.getUniqueId(), user);
    }
  }

  @EventListener
  public void handle(PermissionDeleteUserEvent event) {
    this.permissionManagement.getCachedPermissionUsers().remove(event.getPermissionUser().getUniqueId());
  }

  @EventListener
  public void handle(PermissionAddGroupEvent event) {
    this.permissionManagement.getCachedPermissionGroups()
      .put(event.getPermissionGroup().getName(), event.getPermissionGroup());
  }

  @EventListener
  public void handle(PermissionUpdateGroupEvent event) {
    this.permissionManagement.getCachedPermissionGroups()
      .put(event.getPermissionGroup().getName(), event.getPermissionGroup());
  }

  @EventListener
  public void handle(PermissionDeleteGroupEvent event) {
    this.permissionManagement.getCachedPermissionGroups().remove(event.getPermissionGroup().getName());
  }

  @EventListener
  public void handle(PermissionSetGroupsEvent event) {
    this.permissionManagement.getCachedPermissionGroups().clear();

    for (IPermissionGroup permissionGroup : event.getGroups()) {
      this.permissionManagement.getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
    }
  }
}
