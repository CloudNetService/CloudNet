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

package eu.cloudnetservice.driver.impl.registry;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.resource.ResourceResolver;
import jakarta.inject.Singleton;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the default service registry implementation.
 *
 * @since 4.0
 */
@Singleton
@Provides(ServiceRegistry.class)
public final class DefaultServiceRegistry implements ServiceRegistry {

  final Map<Class<?>, ServiceRegistrationsBinding<?>> serviceBindings = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void discoverServices(@NonNull Class<?> owner) {
    ResourceResolver.openCodeSourceRoot(owner, basePath -> {
      var autoServicesDirectory = basePath.resolve("autoservices");
      FileUtil.walkFileTree(autoServicesDirectory, (_, filePath) -> {
        try (var dataInput = new DataInputStream(Files.newInputStream(filePath, StandardOpenOption.READ))) {
          while (dataInput.available() > 0) {
            var serviceMapping = AutoServiceMapping.deserialize(owner, dataInput);
            if (serviceMapping.singletonService()) {
              // singleton service (= one instance per jvm lifetime), resolve the instance now using injection
              var injectionLayer = InjectionLayer.findLayerOf(owner);
              var serviceInstance = injectionLayer.instance(serviceMapping.implementationType());
              var serviceType = (Class<Object>) serviceMapping.serviceType();
              var registration = this.registerProvider(serviceType, serviceMapping.serviceName(), serviceInstance);
              if (serviceMapping.markServiceAsDefault()) {
                registration.markAsDefaultService();
              }
            } else {
              // constructing provider (= construct a new service instance on each invocation)
              var serviceType = (Class<Object>) serviceMapping.serviceType();
              var implType = (Class<Object>) serviceMapping.implementationType();
              var registration = this.registerConstructingProvider(serviceType, serviceMapping.serviceName(), implType);
              if (serviceMapping.markServiceAsDefault()) {
                registration.markAsDefaultService();
              }
            }
          }
        } catch (IOException | ClassNotFoundException exception) {
          throw new IllegalStateException("Unable to deserialize auto service mappings", exception);
        }
      }, false);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull <S> ServiceRegistryRegistration<S> registerProvider(
    @NonNull Class<S> serviceType,
    @NonNull String serviceName,
    @NonNull S serviceImplementation
  ) {
    Preconditions.checkArgument(!serviceName.isBlank(), "service name cannot be blank");
    Preconditions.checkArgument(serviceType.isInterface(), "service type must be an interface");
    Preconditions.checkArgument(serviceType.isAssignableFrom(serviceImplementation.getClass()), "impl extends service");
    var binding = (ServiceRegistrationsBinding<S>) this.serviceBindings.computeIfAbsent(
      serviceType,
      type -> new ServiceRegistrationsBinding<>(type, this));
    return binding.register(serviceName, serviceImplementation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull <S> ServiceRegistryRegistration<S> registerConstructingProvider(
    @NonNull Class<S> serviceType,
    @NonNull String serviceName,
    @NonNull Class<? extends S> implementationType
  ) {
    Preconditions.checkArgument(!serviceName.isBlank(), "service name cannot be blank");
    Preconditions.checkArgument(serviceType.isInterface(), "service type must be an interface");
    Preconditions.checkArgument(serviceType.isAssignableFrom(implementationType), "impl extends service");
    var binding = (ServiceRegistrationsBinding<S>) this.serviceBindings.computeIfAbsent(
      serviceType,
      type -> new ServiceRegistrationsBinding<>(type, this));
    return binding.register(serviceName, implementationType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterAll(@NonNull ClassLoader classLoader) {
    var iterator = this.serviceBindings.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      var serviceType = entry.getKey();
      var serviceBinding = entry.getValue();
      if (serviceType.getClassLoader() == classLoader) {
        // remove the service binding directly if the service type
        // itself is loaded by the given classloader
        // also calls removeAllRegistrations() on the binding to invalidate
        // all registration objects that might be stored somewhere in some code
        serviceBinding.cleanupAndMarkObsolete();
        iterator.remove();
      } else {
        // remove all bindings that are registered whose implementation types were
        // loaded by the given class loader. this call also removes the service type
        // from the map in case no more registrations are left after the call
        serviceBinding.unregisterAllByClassLoader(classLoader);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  public @NonNull Collection<Class<?>> registeredServiceTypes() {
    return Collections.unmodifiableCollection(this.serviceBindings.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnknownNullability
  @SuppressWarnings("unchecked")
  public <S> ServiceRegistryRegistration<S> registration(@NonNull Class<S> service, @NonNull String name) {
    var binding = (ServiceRegistrationsBinding<S>) this.serviceBindings.get(service);
    return binding != null ? binding.findRegistrationByName(name) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnknownNullability
  @SuppressWarnings("unchecked")
  public <S> ServiceRegistryRegistration<S> defaultRegistration(@NonNull Class<S> service) {
    var binding = (ServiceRegistrationsBinding<S>) this.serviceBindings.get(service);
    return binding != null ? binding.defaultRegistrationProxy() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  @SuppressWarnings("unchecked")
  public @NonNull <S> Collection<ServiceRegistryRegistration<S>> registrations(@NonNull Class<S> service) {
    var binding = (ServiceRegistrationsBinding<S>) this.serviceBindings.get(service);
    return binding != null ? binding.registrations() : List.of();
  }
}
