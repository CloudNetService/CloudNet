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

package eu.cloudnetservice.cloudnet.driver.permission;

import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface CachedPermissionManagement extends PermissionManagement {

  @NonNull Map<UUID, PermissionUser> cachedPermissionUsers();

  @NonNull Map<String, PermissionGroup> cachedPermissionGroups();

  @Nullable PermissionUser cachedUser(@NonNull UUID uniqueId);

  @Nullable PermissionGroup cachedGroup(@NonNull String name);

  void acquireLock(@NonNull PermissionUser user);

  void acquireLock(@NonNull PermissionGroup group);

  boolean locked(@NonNull PermissionUser user);

  boolean locked(@NonNull PermissionGroup group);

  void unlock(@NonNull PermissionUser user);

  void unlock(@NonNull PermissionGroup group);

  void unlockFully(@NonNull PermissionUser user);

  void unlockFully(@NonNull PermissionGroup group);
}
