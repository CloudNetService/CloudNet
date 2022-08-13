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

package eu.cloudnetservice.driver.event.events.permission;

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.Collection;
import lombok.NonNull;

/**
 * An event being fired when the permission groups get updated during a cluster sync or all groups were set using the
 * api.
 *
 * @see PermissionManagement#groups(Collection)
 * @since 4.0
 */
public final class PermissionSetGroupsEvent extends PermissionEvent {

  private final Collection<PermissionGroup> groups;

  /**
   * Constructs a new permission event.
   *
   * @param permissionManagement the permission management associated with this event.
   * @param groups               all groups which are now recognized by the permission management.
   * @throws NullPointerException if either the given management or group collection is null.
   */
  public PermissionSetGroupsEvent(
    @NonNull PermissionManagement permissionManagement,
    @NonNull Collection<PermissionGroup> groups
  ) {
    super(permissionManagement);
    this.groups = groups;
  }

  /**
   * Get all groups which were set during the update and are now recognized by the permission management.
   *
   * @return all groups which were set during the update.
   */
  public @NonNull Collection<PermissionGroup> groups() {
    return this.groups;
  }
}
