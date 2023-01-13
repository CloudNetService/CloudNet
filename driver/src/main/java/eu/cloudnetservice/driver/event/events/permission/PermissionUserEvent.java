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

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import lombok.NonNull;

/**
 * Represents an event which is associated with a permission user.
 *
 * @since 4.0
 */
public abstract class PermissionUserEvent extends PermissionEvent {

  private final PermissionUser permissionUser;

  /**
   * Constructs a new permission user event.
   *
   * @param permissionManagement the permission management associated with this event.
   * @param permissionUser       the permission user associated with this event.
   * @throws NullPointerException if the permission management or user is null.
   */
  public PermissionUserEvent(
    @NonNull PermissionManagement permissionManagement,
    @NonNull PermissionUser permissionUser
  ) {
    super(permissionManagement);
    this.permissionUser = permissionUser;
  }

  /**
   * Get the permission user which is associated with this event.
   *
   * @return the permission user which is associated with this event.
   */
  public @NonNull PermissionUser permissionUser() {
    return this.permissionUser;
  }
}
