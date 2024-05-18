/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import lombok.NonNull;

/**
 * Represents any event which is related to permission handling in any way.
 *
 * @since 4.0
 */
public abstract class PermissionEvent extends Event {

  private final PermissionManagement permissionManagement;

  /**
   * Constructs a new permission event.
   *
   * @param permissionManagement the permission management associated with this event.
   * @throws NullPointerException if the permission management is null.
   */
  public PermissionEvent(@NonNull PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  /**
   * Get the permission management which is associated with this event.
   *
   * @return the permission management which is associated with this event.
   */
  public @NonNull PermissionManagement permissionManagement() {
    return this.permissionManagement;
  }
}
