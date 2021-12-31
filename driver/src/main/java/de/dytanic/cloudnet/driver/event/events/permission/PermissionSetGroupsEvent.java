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

package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import java.util.Collection;
import lombok.NonNull;

public final class PermissionSetGroupsEvent extends PermissionEvent {

  private final Collection<? extends PermissionGroup> groups;

  public PermissionSetGroupsEvent(
    @NonNull PermissionManagement permissionManagement,
    @NonNull Collection<? extends PermissionGroup> groups
  ) {
    super(permissionManagement);
    this.groups = groups;
  }

  public @NonNull Collection<? extends PermissionGroup> groups() {
    return this.groups;
  }
}
