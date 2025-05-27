/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.config.storage;

import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import lombok.NonNull;

/**
 * Resolver for the storage to use for a module configuration. The resolver is loaded from the service registry. The
 * default resolver is always used. Any additional resolver registrations are ignored.
 *
 * @since 4.0
 */
public interface ModuleConfigStorageResolver {

  /**
   * Determines the storage to be used for a module configuration that is created based on the given properties.
   *
   * @param configProperties the configuration properties to resolve the storage for.
   * @return the module config storage to use.
   * @throws NullPointerException     if the given configuration properties instance is null.
   * @throws IllegalArgumentException if the storage for the given configuration properties could not be resolved.
   */
  @NonNull
  ModuleConfigStorage resolveStorage(@NonNull ModuleConfigProperties<?> configProperties);
}
