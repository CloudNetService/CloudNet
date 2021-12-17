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

package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public final class CloudPermissionsHelper {

  private static final Logger LOGGER = LogManager.logger(CloudPermissionsHelper.class);

  private CloudPermissionsHelper() {
    throw new UnsupportedOperationException();
  }

  public static void initPermissionUser(
    @NotNull IPermissionManagement permissionsManagement,
    @NotNull UUID uniqueId,
    @NotNull String name,
    @NotNull Consumer<String> disconnectHandler
  ) {
    initPermissionUser(permissionsManagement, uniqueId, name, disconnectHandler, true);
  }

  public static void initPermissionUser(
    @NotNull IPermissionManagement permissionsManagement,
    @NotNull UUID uniqueId,
    @NotNull String name,
    @NotNull Consumer<String> disconnectHandler,
    boolean shouldUpdateName
  ) {
    PermissionUser permissionUser;
    try {
      permissionUser = permissionsManagement.getOrCreateUserAsync(uniqueId, name).get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      LOGGER.severe("Error while loading permission user: " + uniqueId + "/" + name, exception);
      // disconnect the player now
      disconnectHandler.accept("§cAn internal error while loading your permission profile");
      return;
    }

    var management = asCachedPermissionManagement(permissionsManagement);
    if (management != null) {
      management.acquireLock(permissionUser);
    }

    if (shouldUpdateName && !name.equals(permissionUser.name())) {
      permissionUser.setName(name);
      permissionsManagement.updateUserAsync(permissionUser);
    }
  }

  public static void handlePlayerQuit(IPermissionManagement permissionsManagement, UUID uniqueId) {
    var management = asCachedPermissionManagement(permissionsManagement);
    if (management != null) {
      var cachedUser = management.getCachedUser(uniqueId);
      if (cachedUser != null) {
        management.unlock(cachedUser);
      }
    }
  }

  public static CachedPermissionManagement asCachedPermissionManagement(IPermissionManagement management) {
    return management instanceof CachedPermissionManagement ? (CachedPermissionManagement) management : null;
  }
}
