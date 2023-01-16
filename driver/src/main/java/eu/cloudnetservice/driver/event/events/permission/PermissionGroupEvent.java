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

package eu.cloudnetservice.driver.event.events.permission;

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import lombok.NonNull;

/**
 * Represents an event which is related to a permission group.
 *
 * @since 4.0
 */
public abstract class PermissionGroupEvent extends PermissionEvent {

  private final PermissionGroup permissionGroup;

  /**
   * Constructs a new permission group event.
   *
   * @param permissionManagement the permission management associated with this event.
   * @param permissionGroup      the permission group associated with this event.
   * @throws NullPointerException if either the permission management or group is null.
   */
  public PermissionGroupEvent(
    @NonNull PermissionManagement permissionManagement,
    @NonNull PermissionGroup permissionGroup
  ) {
    super(permissionManagement);
    this.permissionGroup = permissionGroup;
  }

  /**
   * Get the permission group which is associated with this event.
   *
   * @return the permission group which is associated with this event.
   */
  public @NonNull PermissionGroup permissionGroup() {
    return this.permissionGroup;
  }
}
