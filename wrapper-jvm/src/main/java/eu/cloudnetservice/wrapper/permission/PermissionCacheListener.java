/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.permission;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.permission.PermissionAddGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionDeleteGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionDeleteUserEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionSetGroupsEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateUserEvent;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class PermissionCacheListener {

  private final WrapperPermissionManagement permissionManagement;

  public PermissionCacheListener(@NonNull WrapperPermissionManagement permissionManagement) {
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

    for (var permissionGroup : event.groups()) {
      this.permissionManagement.cachedPermissionGroups().put(permissionGroup.name(), permissionGroup);
    }
  }
}
