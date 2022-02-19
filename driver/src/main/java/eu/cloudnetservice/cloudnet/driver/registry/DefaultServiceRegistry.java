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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;

/**
 * Represents the default service registry implementation.
 *
 * @since 4.0
 */
public class DefaultServiceRegistry implements ServiceRegistry {

  protected final Multimap<Class<?>, RegistryEntry<?>> providers = Multimaps.newMultimap(
    new ConcurrentHashMap<>(),
    ConcurrentLinkedQueue::new);

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServiceRegistry registerProvider(
    @NonNull Class<T> service,
    @NonNull String name,
    @NonNull E provider
  ) {
    this.providers.put(service, new RegistryEntry<>(name, provider));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServiceRegistry unregisterProvider(
    @NonNull Class<T> service,
    @NonNull Class<E> provider
  ) {
    // get all registered providers
    var providers = this.providers.get(service);
    if (!providers.isEmpty()) {
      providers.removeIf(entry -> entry.provider().getClass().isAssignableFrom(provider));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends T> @NonNull ServiceRegistry unregisterProvider(@NonNull Class<T> service, @NonNull E provider) {
    var providers = this.providers.get(service);
    if (!providers.isEmpty()) {
      providers.removeIf(entry -> entry.provider().equals(provider));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> boolean hasProvider(@NonNull Class<T> clazz, @NonNull String name) {
    return this.providers.get(clazz).stream().anyMatch(entry -> entry.name().equals(name));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull ServiceRegistry unregisterProvider(@NonNull Class<T> service, @NonNull String name) {
    var providers = this.providers.get(service);
    if (!providers.isEmpty()) {
      providers.removeIf(entry -> entry.name().equals(name));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull ServiceRegistry unregisterProviders(@NonNull Class<T> service) {
    this.providers.removeAll(service);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceRegistry unregisterAll() {
    this.providers.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceRegistry unregisterAll(@NonNull ClassLoader classLoader) {
    for (var entry : this.providers.entries()) {
      if (entry.getValue().provider().getClass().getClassLoader().equals(classLoader)) {
        this.providers.remove(entry.getKey(), entry.getValue());
      }
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Class<?>> providedServices() {
    return Collections.unmodifiableCollection(this.providers.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T provider(@NonNull Class<T> service, @NonNull String name) {
    return this.providers.get(service).stream()
      .filter(entry -> entry.name().equals(name))
      .map(entry -> (T) entry.provider())
      .findFirst()
      .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @NonNull Collection<T> providers(@NonNull Class<T> service) {
    return this.providers.get(service).stream().map(entry -> (T) entry.provider()).toList();
  }

  /**
   * A registered entry in the service registry.
   *
   * @param name     the name of the registered provider.
   * @param provider the provider instance associated with the entry.
   * @param <T>      the type of the registered provider.
   * @since 4.0
   */
  private record RegistryEntry<T>(@NonNull String name, @NonNull T provider) {

  }
}
