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

package de.dytanic.cloudnet.permission.command;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DefaultPermissionUserCommandSender implements IPermissionUserCommandSender {

  protected final IPermissionUser permissionUser;

  protected final IPermissionManagement permissionManagement;

  protected final Queue<String> writtenMessages = new ConcurrentLinkedQueue<>();

  public DefaultPermissionUserCommandSender(IPermissionUser permissionUser,
    IPermissionManagement permissionManagement) {
    this.permissionUser = permissionUser;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public String getName() {
    return this.permissionUser.getName();
  }

  @Override
  public void sendMessage(String message) {
    Preconditions.checkNotNull(message);

    this.writtenMessages.add(message);

    while (this.writtenMessages.size() > 64) {
      this.writtenMessages.poll();
    }
  }

  @Override
  public void sendMessage(String... messages) {
    Preconditions.checkNotNull(messages);

    for (String message : messages) {
      this.sendMessage(message);
    }
  }

  @Override
  public boolean hasPermission(String permission) {
    return this.permissionManagement.hasPermission(this.permissionUser, permission);
  }

  public IPermissionUser getPermissionUser() {
    return this.permissionUser;
  }

  public IPermissionManagement getPermissionManagement() {
    return this.permissionManagement;
  }

  public Queue<String> getWrittenMessages() {
    return this.writtenMessages;
  }
}
