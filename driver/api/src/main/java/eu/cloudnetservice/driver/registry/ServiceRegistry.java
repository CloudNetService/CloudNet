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

import java.util.Collection;
import lombok.NonNull;
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

  @NonNull
  <S, X extends S> ServiceRegistryRegistration<S> registerProvider(
    @NonNull Class<S> serviceType,
    @NonNull String serviceName,
    @NonNull X serviceImplementation);

  @NonNull
  <S> ServiceRegistryRegistration<S> registerProvider(
    @NonNull Class<S> serviceType,
    @NonNull String serviceName,
    @NonNull Class<? extends S> implementationType);

  void unregisterAll(@NonNull ClassLoader classLoader);

  @NonNull
  @UnmodifiableView
  Collection<Class<?>> registeredServiceTypes();

  @UnknownNullability
  <S> ServiceRegistryRegistration<S> registration(@NonNull Class<S> service, @NonNull String name);

  @UnknownNullability
  <S> ServiceRegistryRegistration<S> defaultRegistration(@NonNull Class<S> service);

  @NonNull
  @UnmodifiableView
  <S> Collection<ServiceRegistryRegistration<S>> registrations(@NonNull Class<S> service);

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
  @UnknownNullability
  default <S> S provider(@NonNull Class<S> service, @NonNull String name) {
    return this.registration(service, name).serviceInstance();
  }

  @UnknownNullability
  default <S> S defaultProvider(@NonNull Class<S> service) {
    return this.defaultRegistration(service).serviceInstance();
  }
}
