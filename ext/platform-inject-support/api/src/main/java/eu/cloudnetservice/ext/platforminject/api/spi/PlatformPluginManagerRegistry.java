/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.api.spi;

import eu.cloudnetservice.ext.platforminject.api.PlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.registry.PlatformManagerRegistryHolder;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformPluginManagerRegistry {

  static @NonNull PlatformPluginManagerRegistry registry() {
    return PlatformManagerRegistryHolder.registry();
  }

  boolean hasManager(@NonNull String platformName);

  @Nullable <I, T> PlatformPluginManager<I, T> manager(@NonNull String platformName);

  @NonNull <I, T> PlatformPluginManager<I, T> safeManager(@NonNull String platformName);

  boolean unregisterManager(@NonNull String platformName);

  @NonNull PlatformPluginManagerRegistry registerManager(@NonNull PlatformPluginManagerProvider<?, ?> manager);
}
