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

package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Collection;

public final class PermissionSetUsersEvent extends PermissionEvent {

  private final Collection<? extends IPermissionUser> users;

  public PermissionSetUsersEvent(IPermissionManagement permissionManagement,
    Collection<? extends IPermissionUser> users) {
    super(permissionManagement);

    this.users = users;
  }

  public Collection<? extends IPermissionUser> getUsers() {
    return this.users;
  }
}
