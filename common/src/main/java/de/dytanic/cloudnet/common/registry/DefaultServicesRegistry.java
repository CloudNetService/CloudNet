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

package de.dytanic.cloudnet.common.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
public class DefaultServicesRegistry implements IServicesRegistry {

  protected final Map<Class<?>, List<RegistryEntry<?>>> providedServices = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull IServicesRegistry registerService(
    @NonNull Class<T> clazz,
    @NonNull String name,
    @NonNull E service
  ) {
    this.providedServices.computeIfAbsent(clazz, c -> new CopyOnWriteArrayList<>())
      .add(new RegistryEntry<>(name, service));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull IServicesRegistry unregisterService(
    @NonNull Class<T> clazz,
    @NonNull Class<E> serviceClazz
  ) {
    if (this.providedServices.containsKey(clazz)) {
      for (var registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.service.getClass().equals(serviceClazz)) {
          this.providedServices.get(clazz).remove(registryEntry);

          if (this.providedServices.get(clazz).isEmpty()) {
            this.providedServices.remove(clazz);
          }
        }
      }
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull IServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull E service) {
    if (this.providedServices.containsKey(clazz)) {
      for (var registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.service.equals(service)) {
          this.providedServices.get(clazz).remove(registryEntry);

          if (this.providedServices.get(clazz).isEmpty()) {
            this.providedServices.remove(clazz);
          }

          break;
        }
      }
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> boolean containsService(@NonNull Class<T> clazz, @NonNull String name) {
    if (this.providedServices.containsKey(clazz)) {
      for (var registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.name.equals(name)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull IServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull String name) {

    if (this.providedServices.containsKey(clazz)) {
      for (var registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.name.equals(name)) {
          this.providedServices.get(clazz).remove(registryEntry);

          if (this.providedServices.get(clazz).isEmpty()) {
            this.providedServices.remove(clazz);
          }

          break;
        }
      }
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull IServicesRegistry unregisterServices(@NonNull Class<T> clazz) {
    if (this.providedServices.containsKey(clazz)) {
      this.providedServices.get(clazz).clear();
      this.providedServices.remove(clazz);
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull IServicesRegistry unregisterAll() {
    this.providedServices.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull IServicesRegistry unregisterAll(@NonNull ClassLoader classLoader) {
    for (var item : this.providedServices.values()) {
      item.removeIf(entry -> entry.service.getClass().getClassLoader().equals(classLoader));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Class<?>> providedServices() {
    return Collections.unmodifiableCollection(this.providedServices.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T service(@NonNull Class<T> clazz, @NonNull String name) {
    T value = null;

    if (this.containsService(clazz, name)) {
      for (var registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.name.equals(name)) {
          value = (T) registryEntry.service;
          break;
        }
      }
    }

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @NonNull Collection<T> services(@NonNull Class<T> clazz) {
    Collection<T> collection = new ArrayList<>();
    if (this.providedServices.containsKey(clazz)) {
      for (var entry : this.providedServices.get(clazz)) {
        collection.add((T) entry.service);
      }
    }

    return collection;
  }

  private record RegistryEntry<T>(@NonNull String name, @NonNull T service) {

  }
}
