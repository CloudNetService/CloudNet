/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.config;

import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorage;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageResolver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

/**
 * Default implementation of a module config storage resolver. The storage implementations are resolved from the service
 * registry, either by override name or the default implementation if none was requested.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ModuleConfigStorageResolver.class, name = "default")
public final class DefaultModuleConfigStorageResolver implements ModuleConfigStorageResolver {

  private final ServiceRegistry serviceRegistry;

  @Inject
  public DefaultModuleConfigStorageResolver(@NonNull ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleConfigStorage resolveStorage(@NonNull ModuleConfigProperties<?> configProperties) {
    var storageName = configProperties.storageOverride();
    var storage = storageName == null
      ? this.serviceRegistry.defaultInstance(ModuleConfigStorage.class)
      : this.serviceRegistry.instance(ModuleConfigStorage.class, storageName);
    if (storage == null) {
      throw new IllegalArgumentException("Unable to resolve module config storage " + storageName);
    }

    return storage;
  }
}
