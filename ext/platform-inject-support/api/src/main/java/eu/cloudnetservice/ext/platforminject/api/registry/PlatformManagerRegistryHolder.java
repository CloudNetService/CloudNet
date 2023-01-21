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

package eu.cloudnetservice.ext.platforminject.api.registry;

import eu.cloudnetservice.ext.platforminject.api.spi.PlatformPluginManagerRegistry;
import java.util.Objects;
import java.util.ServiceLoader;
import lombok.NonNull;

public final class PlatformManagerRegistryHolder {

  private static PlatformPluginManagerRegistry registry;

  public PlatformManagerRegistryHolder() {
    throw new UnsupportedOperationException();
  }

  public static void init(@NonNull ClassLoader classLoader) {
    // load the registry if not yet initialized
    if (registry == null) {
      // construct a service loader which uses all services known to the system class loader
      var serviceLoader = ServiceLoader.load(PlatformPluginManagerRegistry.class, classLoader);
      registry = serviceLoader.findFirst().orElseThrow();
    }
  }

  public static @NonNull PlatformPluginManagerRegistry registry() {
    return Objects.requireNonNull(registry, "registry not yet initialized");
  }
}
