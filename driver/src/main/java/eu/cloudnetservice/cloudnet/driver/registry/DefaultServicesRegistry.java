/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.registry;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
public class DefaultServicesRegistry implements ServicesRegistry {

  protected final Multimap<Class<?>, RegistryEntry<?>> providedServices = Multimaps.newMultimap(
    new ConcurrentHashMap<>(),
    ConcurrentLinkedQueue::new);

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServicesRegistry registerService(
    @NonNull Class<T> clazz,
    @NonNull String name,
    @NonNull E service
  ) {
    this.providedServices.put(clazz, new RegistryEntry<>(name, service));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServicesRegistry unregisterService(
    @NonNull Class<T> clazz,
    @NonNull Class<E> serviceClazz
  ) {
    // get all registered services
    var services = this.providedServices.get(clazz);
    if (!services.isEmpty()) {
      services.removeIf(entry -> entry.service.getClass().equals(serviceClazz));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull E service) {
    var services = this.providedServices.get(clazz);
    if (!services.isEmpty()) {
      services.removeIf(entry -> entry.service.equals(service));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> boolean containsService(@NonNull Class<T> clazz, @NonNull String name) {
    var services = this.providedServices.get(clazz);
    if (!services.isEmpty()) {
      for (var service : services) {
        if (service.name.equals(name)) {
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
  public <T> @NonNull ServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull String name) {
    var services = this.providedServices.get(clazz);
    if (!services.isEmpty()) {
      services.removeIf(entry -> entry.name.equals(name));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull ServicesRegistry unregisterServices(@NonNull Class<T> clazz) {
    this.providedServices.removeAll(clazz);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServicesRegistry unregisterAll() {
    this.providedServices.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServicesRegistry unregisterAll(@NonNull ClassLoader classLoader) {
    for (var entry : this.providedServices.entries()) {
      if (entry.getValue().service().getClass().getClassLoader().equals(classLoader)) {
        this.providedServices.remove(entry.getKey(), entry.getValue());
      }
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
