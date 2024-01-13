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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The service registry manages and provides access to service providers. Services are interfaces or abstract classes
 * which define a set of methods a provider of a service must implement. Modules and/or plugins can query
 * implementations from the registry (if one is available for the given service). If multiple service providers are
 * present for a service the plugin/module must either decide which service to use or should use the first one (first
 * registered one).
 *
 * @since 4.0
 */
public interface ServiceRegistry {

  /**
   * Get the first provider for the given service class. This method returns null when no service for the given class is
   * present. If multiple providers are present the first registered one will be returned.
   *
   * @param service the service to query.
   * @param <T>     the type of the service to query.
   * @return the last registered provider for the given service or null if no provider is registered.
   * @throws NullPointerException if the given service class is null.
   * @deprecated the method is deprecated as it requires abusing the purpose of dependency injection in order to work.
   * There are two ways to replace this part of the api
   * <ul>
   *   <li>injecting the service registry itself and calling {@link #firstProvider(Class)}.</li>
   *   <li>injecting the requested service directly using the {@link eu.cloudnetservice.driver.registry.injection.Service} annotation.</li>
   * </ul>
   */
  @Deprecated(since = "4.0", forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "4.1")
  static <T> @UnknownNullability T first(@NonNull Class<T> service) {
    var registry = InjectionLayer.boot().instance(ServiceRegistry.class);
    return registry.firstProvider(service);
  }

  /**
   * Registers the given provider for the given service to this service registry. After registration the given provider
   * will be the first provider of the given service and retrievable with the given name.
   * <p>
   * If multiple services with the same name are registered the first one will be returned which was registered to this
   * registry.
   *
   * @param service  the service class to register the implementation to.
   * @param name     the name of the provider to register.
   * @param provider the provider (and service implementation) to register.
   * @param <T>      the service class type to register the service to.
   * @param <E>      the provider (or service implementation) class type.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given service class, name or implementation is null.
   */
  @NonNull <T, E extends T> ServiceRegistry registerProvider(
    @NonNull Class<T> service,
    @NonNull String name,
    @NonNull E provider);

  /**
   * Unregisters all providers for the given service which are an instance of the given class.
   *
   * @param service  the service to unregister the providers from.
   * @param provider the class of the providers to unregister.
   * @param <T>      the type of the service.
   * @param <E>      the type of the providers.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given service or provider class is null.
   */
  @NonNull <T, E extends T> ServiceRegistry unregisterProvider(@NonNull Class<T> service, @NonNull Class<E> provider);

  /**
   * Unregisters the given provider instance from the registered services of the given service class.
   *
   * @param service  the service to unregister the provider from.
   * @param provider the provider instance to unregister.
   * @param <T>      the type of the service.
   * @param <E>      the type of the providers.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given service class or provider instance is null.
   */
  @NonNull <T, E extends T> ServiceRegistry unregisterProvider(@NonNull Class<T> service, @NonNull E provider);

  /**
   * Checks if a provider with the given name is registered for the given service.
   *
   * @param service the service to check the providers of.
   * @param name    the name of the service to check for.
   * @param <T>     the type of the service.
   * @return true if this registry has a provider with the given name for the service, false otherwise.
   * @throws NullPointerException if the given service class or provider name is null.
   */
  <T> boolean hasProvider(@NonNull Class<T> service, @NonNull String name);

  /**
   * Unregisters the provider with the given name from the given service if registered previously.
   *
   * @param service the service to unregister the provider from.
   * @param name    the name of the provider to unregister.
   * @param <T>     the type of the service.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given service class or provider name is null.
   */
  @NonNull <T> ServiceRegistry unregisterProvider(@NonNull Class<T> service, @NonNull String name);

  /**
   * Removes all previously registered providers for the given service.
   *
   * @param service the service to unregisters the providers of.
   * @param <T>     the type of the service.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given service is null.
   */
  @NonNull <T> ServiceRegistry unregisterProviders(@NonNull Class<T> service);

  /**
   * Unregisters all previously registered providers for all services from this registry.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @NonNull ServiceRegistry unregisterAll();

  /**
   * Unregisters all providers for any service from this registry whose classes were loaded by the given class loader.
   *
   * @param classLoader the class loader of the providers to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull ServiceRegistry unregisterAll(@NonNull ClassLoader classLoader);

  /**
   * Get all services for which a provider was registered previously.
   *
   * @return all services for which a provider was registered previously.
   */
  @NonNull Collection<Class<?>> providedServices();

  /**
   * Get the provider for the given service with the given name from this registry. This method returns null if no such
   * provider was registered previously.
   * <p>
   * This method will return the first provider registered with the given name if multiple providers with the same name
   * for the given service were registered.
   *
   * @param service the service of the provider.
   * @param name    the name of the provider to get.
   * @param <T>     the type of the service.
   * @return the provider for the given service with the given name, null if no such provider was registered.
   * @throws NullPointerException if the given service or provider name is null.
   */
  @UnknownNullability <T> T provider(@NonNull Class<T> service, @NonNull String name);

  /**
   * Get the first registered provider for the given service from this registry. This method returns null if no provider
   * for the given service is registered.
   *
   * @param service the service to get the first provider of.
   * @param <T>     the type of the service.
   * @return the first registered provider for the given service, null if no providers for the service are registered.
   * @throws NullPointerException if the given service is null.
   */
  default @UnknownNullability <T> T firstProvider(@NonNull Class<T> service) {
    return Iterables.getFirst(this.providers(service), null);
  }

  /**
   * Get all registered providers for the given service. This method returns an empty collection if no providers are
   * registered for the given service but never null.
   *
   * @param service the service to get all registered providers of.
   * @param <T>     the type of the service.
   * @return all registered providers for the service or an empty collection if no providers were registered for it.
   * @throws NullPointerException if the given service is null.
   */
  @UnmodifiableView
  @NonNull <T> Collection<T> providers(@NonNull Class<T> service);
}
