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

import eu.cloudnetservice.driver.module.ModuleConfigKey;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.module.config.ModuleConfigContainer;
import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import eu.cloudnetservice.node.module.config.ModuleConfigRegistry;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageResolver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of a module config registry.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ModuleConfigRegistry.class, name = "default")
public final class DefaultModuleConfigRegistry implements ModuleConfigRegistry {

  private final ServiceRegistry serviceRegistry;

  private final Lock containerRegisterLock = new ReentrantLock(true);
  private final Map<Type, ModuleConfigContainer<?>> containersByType = new ConcurrentHashMap<>();
  private final Map<ModuleConfigKey, ModuleConfigContainer<?>> containersByKey = new ConcurrentHashMap<>();

  @Inject
  public DefaultModuleConfigRegistry(@NonNull ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<ModuleConfigContainer<?>> registeredConfigurations() {
    return this.containersByKey.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <T> ModuleConfigContainer<T> configContainer(@NonNull ModuleConfigKey key) {
    return (ModuleConfigContainer<T>) this.containersByKey.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <T> ModuleConfigContainer<T> configContainer(@NonNull Type configurationModel) {
    return (ModuleConfigContainer<T>) this.containersByType.get(configurationModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public @NonNull <T> ModuleConfigContainer<T> configContainer(@NonNull ModuleConfigProperties<T> properties) {
    this.containerRegisterLock.lock();
    try {
      // check if the container is already registered by key - return the container in that case
      var existingContainer = (ModuleConfigContainer<T>) this.containersByKey.get(properties.key());
      if (existingContainer != null) {
        return existingContainer;
      }

      // ensure that no container with the same model type is registered to a different key
      var existingContainerByType = this.containersByType.get(properties.configModelType());
      if (existingContainerByType != null) {
        var msg = String.format(
          "A container for model '%s' is already registered using key '%s'",
          properties.configModelType(), existingContainerByType.key());
        throw new IllegalArgumentException(msg);
      }

      // resolve the container storage, then construct the new container and register it
      var storageResolver = this.serviceRegistry.defaultInstance(ModuleConfigStorageResolver.class);
      var storage = storageResolver.resolveStorage(properties);
      var configContainer = switch (properties.key().compositeKey()) {
        case true -> {
          DefaultCompositeModuleConfigContainer.validateConfigModelType(properties.configModelType());
          yield (ModuleConfigContainer<T>) new DefaultCompositeModuleConfigContainer(storage, properties);
        }
        case false -> new DefaultSingleModuleConfigContainer<>(storage, properties);
      };
      this.containersByKey.put(properties.key(), configContainer);
      this.containersByType.put(properties.configModelType(), configContainer);
      return configContainer;
    } finally {
      this.containerRegisterLock.unlock();
    }
  }
}
