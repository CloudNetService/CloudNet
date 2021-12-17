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

package de.dytanic.cloudnet.driver.permission;

import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CachedPermissionManagement extends IPermissionManagement {

  @NotNull Map<UUID, PermissionUser> cachedPermissionUsers();

  @NotNull Map<String, PermissionGroup> cachedPermissionGroups();

  @Nullable PermissionUser cachedUser(@NotNull UUID uniqueId);

  @Nullable PermissionGroup cachedGroup(@NotNull String name);

  void acquireLock(@NotNull PermissionUser user);

  void acquireLock(@NotNull PermissionGroup group);

  boolean locked(@NotNull PermissionUser user);

  boolean locked(@NotNull PermissionGroup group);

  void unlock(@NotNull PermissionUser user);

  void unlock(@NotNull PermissionGroup group);

  void unlockFully(@NotNull PermissionUser user);

  void unlockFully(@NotNull PermissionGroup group);
}
