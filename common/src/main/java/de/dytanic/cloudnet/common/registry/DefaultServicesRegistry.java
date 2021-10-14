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

/**
 * This is the default implementation of the IServicesRegistry class.
 *
 * @see IServicesRegistry
 */
public class DefaultServicesRegistry implements IServicesRegistry {

  protected final Map<Class<?>, List<RegistryEntry<?>>> providedServices = new ConcurrentHashMap<>();

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T, E extends T> IServicesRegistry registerService(Class<T> clazz, String name, E service) {
    if (clazz == null || name == null || service == null) {
      return this;
    }

    this.providedServices.computeIfAbsent(clazz, c -> new CopyOnWriteArrayList<>())
      .add(new RegistryEntry<>(name, service));
    return this;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T, E extends T> IServicesRegistry unregisterService(Class<T> clazz, Class<E> serviceClazz) {
    if (clazz == null || serviceClazz == null) {
      return this;
    }

    if (this.providedServices.containsKey(clazz)) {
      for (RegistryEntry<?> registryEntry : this.providedServices.get(clazz)) {
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
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T, E extends T> IServicesRegistry unregisterService(Class<T> clazz, E service) {
    if (clazz == null || service == null) {
      return this;
    }

    if (this.providedServices.containsKey(clazz)) {
      for (RegistryEntry<?> registryEntry : this.providedServices.get(clazz)) {
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
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T> boolean containsService(Class<T> clazz, String name) {
    if (clazz == null || name == null) {
      return false;
    }

    if (this.providedServices.containsKey(clazz)) {
      for (RegistryEntry<?> registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.name.equals(name)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T> IServicesRegistry unregisterService(Class<T> clazz, String name) {

    if (this.providedServices.containsKey(clazz)) {
      for (RegistryEntry<?> registryEntry : this.providedServices.get(clazz)) {
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
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T> IServicesRegistry unregisterServices(Class<T> clazz) {
    if (this.providedServices.containsKey(clazz)) {
      this.providedServices.get(clazz).clear();
      this.providedServices.remove(clazz);
    }

    return this;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public IServicesRegistry unregisterAll() {
    this.providedServices.clear();
    return this;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public IServicesRegistry unregisterAll(ClassLoader classLoader) {
    for (List<RegistryEntry<?>> item : this.providedServices.values()) {
      item.removeIf(entry -> entry.service.getClass().getClassLoader().equals(classLoader));
    }

    return this;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public Collection<Class<?>> getProvidedServices() {
    return Collections.unmodifiableCollection(this.providedServices.keySet());
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T> T getService(Class<T> clazz, String name) {
    if (clazz == null || name == null) {
      return null;
    }

    T value = null;

    if (this.containsService(clazz, name)) {
      for (RegistryEntry<?> registryEntry : this.providedServices.get(clazz)) {
        if (registryEntry.name.equals(name)) {
          value = (T) registryEntry.service;
          break;
        }
      }
    }

    return value;
  }

  /**
   * Default implementation of IServiceRegistry class
   *
   * @see IServicesRegistry
   */
  @Override
  public <T> Collection<T> getServices(Class<T> clazz) {
    Collection<T> collection = new ArrayList<>();

    if (clazz == null) {
      return collection;
    }

    if (this.providedServices.containsKey(clazz)) {
      for (RegistryEntry<?> entry : this.providedServices.get(clazz)) {
        collection.add((T) entry.service);
      }
    }

    return collection;
  }


  public static class RegistryEntry<T> {

    final String name;

    final T service;


    public RegistryEntry(String name, T service) {
      this.name = name;
      this.service = service;
    }
  }
}
