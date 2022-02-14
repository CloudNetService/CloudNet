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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The ServicesRegistry, is to be able to manage and summarize implementations of interfaces and abstract classes in
 * order to make them dynamically retrievable.
 */
public interface ServicesRegistry {

  /**
   * Returns the implementation of the given service from the default service registry.
   *
   * @param clazz the class that was used to register the service in the registry.
   * @param <T>   the interface or class type of the registered service.
   * @return the current service instance of the given class or null if none was registered.
   * @throws NullPointerException if the given class is null.
   * @see CloudNetDriver#servicesRegistry()
   */
  static <T> @UnknownNullability T first(@NonNull Class<T> clazz) {
    return CloudNetDriver.instance().servicesRegistry().firstService(clazz);
  }

  /**
   * Registers a new service from the basic parent class. and can get with the following key
   *
   * @param clazz   the interface class, which should the provider of the service
   * @param name    the name of the service, which should registered
   * @param service the service instance to register
   * @param <T>     the interface or abstract type which you want the implementation providing for
   * @param <E>     the implementation of the service type T
   * @return the current instance, of the class, which was used to offerTask this method
   */
  @NonNull <T, E extends T> ServicesRegistry registerService(
    @NonNull Class<T> clazz,
    @NonNull String name,
    @NonNull E service);

  /**
   * Unregister all services which class equals the E type of the registered services
   *
   * @param clazz   the interface class, which should the provider of the service
   * @param service the class, of the services which should unregister
   * @param <T>     the interface or abstract type which you want the implementation providing for
   * @param <E>     the implementation of the service type T
   * @return the current instance of the ServiceRegistry
   */
  @NonNull <T, E extends T> ServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull Class<E> service);

  /**
   * Unregister all services which instance is equals one of this services that are already registered
   *
   * @param clazz   the interface class, which should the provider of the service
   * @param service the instance
   * @param <T>     the interface or abstract type which you want the implementation providing for
   * @param <E>     the implementation of the service type T
   * @return the current instance of the ServiceRegistry
   */
  @NonNull <T, E extends T> ServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull E service);

  /**
   * Request of a service with a specific name is already exists or not
   *
   * @param clazz the provider class
   * @param name  the name of the service, from that should be check
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return true if a service from the provider is contain in this registry
   */
  <T> boolean containsService(@NonNull Class<T> clazz, @NonNull String name);

  /**
   * Unregisters a service by the name of the service on a specific class provider
   *
   * @param clazz the base class from that the service should be removed
   * @param name  the registered name from the service implementation
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return the current instance of the ServiceRegistry
   */
  @NonNull <T> ServicesRegistry unregisterService(@NonNull Class<T> clazz, @NonNull String name);

  /**
   * Removes all services from the registry, which has the following provider class
   *
   * @param clazz the provider base class from that should all services be removed
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return the current instance of the ServiceRegistry
   */
  @NonNull <T> ServicesRegistry unregisterServices(@NonNull Class<T> clazz);

  /**
   * Unregister all services and provider classes from the registry
   *
   * @return the current instance of the ServiceRegistry
   */
  @NonNull ServicesRegistry unregisterAll();

  /**
   * Unregister all services which are loaded from that specific classLoader.
   *
   * @param classLoader the classLoader, from that all services should be removed
   * @return the current instance of the ServiceRegistry
   */
  @NonNull ServicesRegistry unregisterAll(@NonNull ClassLoader classLoader);

  /**
   * Returns all provider classes that are actual contain in on this registry
   */
  @NonNull Collection<Class<?>> providedServices();

  /**
   * Returns the service implementation or null from this registry
   *
   * @param clazz the base provider class, from that the service should get
   * @param name  the name of the service that should get
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return the service implementation of the base class or the base class himself
   */
  @UnknownNullability <T> T service(@NonNull Class<T> clazz, @NonNull String name);

  /**
   * Returns the service implementation or null from this registry
   *
   * @param clazz the base provider class, from that the service should get
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return the first service implementation found registered for the provided class
   */
  default @UnknownNullability <T> T firstService(@NonNull Class<T> clazz) {
    return Iterables.getFirst(this.services(clazz), null);
  }

  /**
   * Returns all services implementation
   *
   * @param clazz the base provider class, from that the service should get
   * @param <T>   the interface or abstract type which you want the implementation providing for
   * @return the service implementations of the base class or the base class himself
   */
  @UnmodifiableView
  @NonNull <T> Collection<T> services(@NonNull Class<T> clazz);
}
