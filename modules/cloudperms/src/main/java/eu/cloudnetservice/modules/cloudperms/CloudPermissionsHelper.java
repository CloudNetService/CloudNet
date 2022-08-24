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

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.permission.CachedPermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
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
    @NonNull Consumer<String> disconnectHandler,
    boolean shouldUpdateName
  ) {
    PermissionUser permissionUser;
    try {
      permissionUser = permissionsManagement.getOrCreateUserAsync(uniqueId, name).get(5, TimeUnit.SECONDS);
    } catch (ExecutionException | TimeoutException exception) {
      LOGGER.severe("Error while loading permission user: " + uniqueId + "/" + name, exception);
      // disconnect the player now
      disconnectHandler.accept("§cAn internal error while loading your permission profile");
      return;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt(); // reset the interrupted state of the thread
      return;
    }

    if (permissionsManagement instanceof CachedPermissionManagement cached) {
      cached.acquireLock(permissionUser);
    }

    if (shouldUpdateName && !name.equals(permissionUser.name())) {
      permissionsManagement.modifyUser(uniqueId, ($, builder) -> builder.name(name));
    }
  }

  public static void handlePlayerQuit(@Nullable PermissionManagement permissionsManagement, @NonNull UUID uniqueId) {
    if (permissionsManagement instanceof CachedPermissionManagement cached) {
      var cachedUser = cached.cachedUser(uniqueId);
      if (cachedUser != null) {
        cached.unlock(cachedUser);
      }
    }
  }
}
