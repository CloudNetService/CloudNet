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

package eu.cloudnetservice.driver.registry;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Holds the singleton instance of the service registry which is returned by {@link ServiceRegistry#registry()}. The
 * instance is retrieved from the boot injection layer upon the first invocation of the mentioned method and will not
 * change over the lifetime of the jvm. Injecting the service registry should yield the same result as using the static
 * accessor method. The static method is more of a helper function for internal code which has no direct access to
 * dependency injection, for example to construct builder instances, but still want to facility the instance retrieval
 * for the user.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class ServiceRegistryHolder {

  private static ServiceRegistry instance;

  private ServiceRegistryHolder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the jvm-static instance of the service registry, retrieving it from the boot injection layer if the current
   * call is the first call to the method.
   *
   * @return the jvm-static instance of the service registry, retrieved from the boot injection layer.
   * @see ServiceRegistry#registry()
   */
  static @NonNull ServiceRegistry instance() {
    if (instance == null) {
      // instance is not yet initialized, do that now
      // it doesn't matter if this code is executed twice, the singleton
      // character of the service registry is manged by our dependency injection
      instance = InjectionLayer.boot().instance(ServiceRegistry.class);
    }

    return instance;
  }
}
