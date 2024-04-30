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

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import lombok.NonNull;

/**
 * An event being called when a permission group gets deleted.
 *
 * @since 4.0
 */
public final class PermissionDeleteGroupEvent extends PermissionGroupEvent {

  /**
   * Constructs a new permission delete group event.
   *
   * @param permissionManagement the permission management associated with this event.
   * @param permissionGroup      the permission group associated with this event.
   * @throws NullPointerException if either the permission management or group is null.
   */
  public PermissionDeleteGroupEvent(
    @NonNull PermissionManagement permissionManagement,
    @NonNull PermissionGroup permissionGroup
  ) {
    super(permissionManagement, permissionGroup);
  }
}
