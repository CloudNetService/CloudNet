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
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public class DefaultServicesRegistry implements IServicesRegistry {

  protected final Map<Class<?>, List<RegistryEntry<?>>> providedServices = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NotNull IServicesRegistry registerService(
    @NotNull Class<T> clazz,
    @NotNull String name,
    @NotNull E service
  ) {
    this.providedServices.computeIfAbsent(clazz, c -> new CopyOnWriteArrayList<>())
      .add(new RegistryEntry<>(name, service));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NotNull IServicesRegistry unregisterService(
    @NotNull Class<T> clazz,
    @NotNull Class<E> serviceClazz
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
  public <T, E extends T> @NotNull IServicesRegistry unregisterService(@NotNull Class<T> clazz, @NotNull E service) {
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
  public <T> boolean containsService(@NotNull Class<T> clazz, @NotNull String name) {
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
  public <T> @NotNull IServicesRegistry unregisterService(@NotNull Class<T> clazz, @NotNull String name) {

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
  public <T> @NotNull IServicesRegistry unregisterServices(@NotNull Class<T> clazz) {
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
  public @NotNull IServicesRegistry unregisterAll() {
    this.providedServices.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IServicesRegistry unregisterAll(@NotNull ClassLoader classLoader) {
    for (var item : this.providedServices.values()) {
      item.removeIf(entry -> entry.service.getClass().getClassLoader().equals(classLoader));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<Class<?>> getProvidedServices() {
    return Collections.unmodifiableCollection(this.providedServices.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getService(@NotNull Class<T> clazz, @NotNull String name) {
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
  public <T> @NotNull Collection<T> getServices(@NotNull Class<T> clazz) {
    Collection<T> collection = new ArrayList<>();
    if (this.providedServices.containsKey(clazz)) {
      for (var entry : this.providedServices.get(clazz)) {
        collection.add((T) entry.service);
      }
    }

    return collection;
  }


  private static final class RegistryEntry<T> {

    private final T service;
    private final String name;

    public RegistryEntry(@NotNull String name, @NotNull T service) {
      this.name = name;
      this.service = service;
    }
  }
}
