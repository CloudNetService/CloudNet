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

package eu.cloudnetservice.modules.cloudperms;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.permission.CachedPermissionManagement;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionUser;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class CloudPermissionsHelper {

  private static final Logger LOGGER = LogManager.logger(CloudPermissionsHelper.class);

  private CloudPermissionsHelper() {
    throw new UnsupportedOperationException();
  }

  public static void initPermissionUser(
    @NonNull PermissionManagement permissionsManagement,
    @NonNull UUID uniqueId,
    @NonNull String name,
    @NonNull Consumer<String> disconnectHandler
  ) {
    initPermissionUser(permissionsManagement, uniqueId, name, disconnectHandler, true);
  }

  public static void initPermissionUser(
    @NonNull PermissionManagement permissionsManagement,
    @NonNull UUID uniqueId,
    @NonNull String name,
    @NonNull Consumer<String> disconnectHandler,
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
      permissionsManagement.modifyUser(uniqueId, ($, builder) -> builder.name(name));
    }
  }

  public static void handlePlayerQuit(@Nullable PermissionManagement permissionsManagement, @NonNull UUID uniqueId) {
    var management = asCachedPermissionManagement(permissionsManagement);
    if (management != null) {
      var cachedUser = management.cachedUser(uniqueId);
      if (cachedUser != null) {
        management.unlock(cachedUser);
      }
    }
  }

  public static @Nullable CachedPermissionManagement asCachedPermissionManagement(
    @Nullable PermissionManagement management
  ) {
    return management instanceof CachedPermissionManagement ? (CachedPermissionManagement) management : null;
  }
}
