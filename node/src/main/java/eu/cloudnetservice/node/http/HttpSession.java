/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.http;

import eu.cloudnetservice.driver.permission.PermissionUser;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface HttpSession {

  long expireTime();

  long refreshFor(long liveMillis);

  @NonNull String uniqueId();

  @NonNull UUID userId();

  @UnknownNullability PermissionUser user();

  <T> @UnknownNullability T property(@NonNull String key);

  <T> @UnknownNullability T property(@NonNull String key, @Nullable T def);

  @NonNull HttpSession setProperty(@NonNull String key, @NonNull Object value);

  @NonNull HttpSession removeProperty(@NonNull String key);

  boolean hasProperty(@NonNull String key);

  @NonNull Map<String, Object> properties();

  @NonNull V2HttpAuthentication issuer();
}
