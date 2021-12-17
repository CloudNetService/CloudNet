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
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class PermissionCacheListener {

  private final WrapperPermissionManagement permissionManagement;

  public PermissionCacheListener(WrapperPermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateUserEvent event) {
    var user = event.permissionUser();
    if (this.permissionManagement.cachedPermissionUsers().containsKey(user.uniqueId())) {
      this.permissionManagement.cachedPermissionUsers().put(user.uniqueId(), user);
    }
  }

  @EventListener
  public void handle(@NonNull PermissionDeleteUserEvent event) {
    this.permissionManagement.cachedPermissionUsers().remove(event.permissionUser().uniqueId());
  }

  @EventListener
  public void handle(@NonNull PermissionAddGroupEvent event) {
    this.permissionManagement.cachedPermissionGroups().put(
      event.permissionGroup().name(),
      event.permissionGroup());
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateGroupEvent event) {
    this.permissionManagement.cachedPermissionGroups().put(
      event.permissionGroup().name(),
      event.permissionGroup());
  }

  @EventListener
  public void handle(@NonNull PermissionDeleteGroupEvent event) {
    this.permissionManagement.cachedPermissionGroups().remove(event.permissionGroup().name());
  }

  @EventListener
  public void handle(@NonNull PermissionSetGroupsEvent event) {
    this.permissionManagement.cachedPermissionGroups().clear();

    for (PermissionGroup permissionGroup : event.groups()) {
      this.permissionManagement.cachedPermissionGroups().put(permissionGroup.name(), permissionGroup);
    }
  }
}
