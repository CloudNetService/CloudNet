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

package de.dytanic.cloudnet.http.v2;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpSession {

  long getExpireTime();

  long refreshFor(long liveMillis);

  @NotNull String getUniqueId();

  @NotNull UUID getUserId();

  IPermissionUser getUser();

  <T> T getProperty(@NotNull String key);

  <T> T getProperty(@NotNull String key, @Nullable T def);

  @NotNull HttpSession setProperty(@NotNull String key, @NotNull Object value);

  @NotNull HttpSession removeProperty(@NotNull String key);

  boolean hasProperty(@NotNull String key);

  @NotNull Map<String, Object> getProperties();
}
