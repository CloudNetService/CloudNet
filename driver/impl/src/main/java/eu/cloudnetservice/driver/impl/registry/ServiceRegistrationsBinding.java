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
import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

final class ServiceRegistrationsBinding<S> {

  private final Lock readLock;
  private final Lock writeLock;

  private final Class<S> serviceType;
  private final DefaultServiceRegistry registry;
  private final ServiceRegistryRegistration<S> defaultRegistrationProxy;
  private final SequencedMap<String, ServiceRegistryRegistration<S>> registrationsByName;

  private volatile boolean obsolete;
  private volatile ServiceRegistryRegistration<S> defaultRegistrationRef;

  public ServiceRegistrationsBinding(@NonNull Class<S> serviceType, @NonNull DefaultServiceRegistry serviceRegistry) {
    var rwLock = new ReentrantReadWriteLock(true);
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();

    this.serviceType = serviceType;
    this.registry = serviceRegistry;
    this.registrationsByName = new LinkedHashMap<>();
    this.defaultRegistrationProxy = new ProxiedServiceRegistration<>(
      serviceType,
      this,
      () -> this.executeInReadLock(() -> this.defaultRegistrationRef));
  }

  public @NonNull ServiceRegistryRegistration<S> defaultRegistrationProxy() {
    return this.defaultRegistrationProxy;
  }

  public boolean valid() {
    return this.executeInReadLock(() -> !this.obsolete);
  }

  public boolean registrationValid(@NonNull ServiceRegistryRegistration<S> registration) {
    return this.executeInReadLock(() -> !this.obsolete && this.registrationsByName.containsValue(registration));
  }

  public boolean registrationIsDefault(@NonNull ServiceRegistryRegistration<S> registration) {
    return this.executeInReadLock(() -> !this.obsolete && this.defaultRegistrationRef == registration);
  }

  public void markAsDefaultRegistration(@NonNull ServiceRegistryRegistration<S> registration) {
    this.executeInWriteLock(() -> {
      Preconditions.checkState(this.registrationsByName.containsValue(registration), "registration no longer valid");
      this.defaultRegistrationRef = registration;
      return null;
    });
  }

  public @Nullable ServiceRegistryRegistration<S> findRegistrationByName(@NonNull String serviceName) {
    return this.executeInReadLock(() -> this.registrationsByName.get(serviceName));
  }

  public @NonNull ServiceRegistryRegistration<S> register(
    @NonNull String serviceName,
    @NonNull S serviceImplementation
  ) {
    var registration = this.register(
      serviceName,
      () -> new FixedInstanceServiceRegistration<>(this.serviceType, serviceName, serviceImplementation, this));
    return Objects.requireNonNullElseGet(
      registration,
      () -> this.registry.registerProvider(this.serviceType, serviceName, serviceImplementation));
  }

  public @NonNull ServiceRegistryRegistration<S> register(
    @NonNull String serviceName,
    @NonNull Class<? extends S> implementationType
  ) {
    var registration = this.register(serviceName, () -> {
      try {
        var lookup = MethodHandles.publicLookup();
        var noArgConstructorType = MethodType.methodType(void.class);
        var genericNoArgConstructorHandle = lookup.findConstructor(implementationType, noArgConstructorType)
          .asType(noArgConstructorType.changeReturnType(Object.class))
          .asFixedArity();
        return new NewInstanceServiceRegistration<>(this.serviceType, serviceName, genericNoArgConstructorHandle, this);
      } catch (NoSuchMethodException exception) {
        // no no-args constructor exists in the implementation type
        throw new IllegalArgumentException("Service implementation must have a public no-args constructor");
      } catch (IllegalAccessException exception) {
        // the constructor or class is either not public or the package is not open for us to access it
        throw new IllegalArgumentException("Service implementation must have a public & accessible no-arg constructor");
      }
    });
    return Objects.requireNonNullElseGet(
      registration,
      () -> this.registry.registerProvider(this.serviceType, serviceName, implementationType));
  }

  public @Nullable ServiceRegistryRegistration<S> register(
    @NonNull String serviceName,
    @NonNull Supplier<ServiceRegistryRegistration<S>> registrationFactory
  ) {
    return this.executeInWriteLock(() -> {
      // this might happen due to a race between unregistering the last provider from this binding
      // and a registration call that is happening to the service registry as the last unregister
      // will mark this binding as obsolete and remove it from the map in the service registry. if
      // this happens we'll just call the registry again for a new register as that will create a
      // new binding and register into that new binding instead
      if (this.obsolete) {
        return null;
      }

      // get the existing or register a new service registration, just by using the name
      // this does not, and should not, validate if the implementation was already used for a different registration
      var registration = this.registrationsByName.computeIfAbsent(serviceName, _ -> registrationFactory.get());

      // set the default registration to the given registration in case no default registration is set yet
      // this should only happen for the registration that is being registered, all subsequent invocations
      // should not see a null value on the default registration field
      if (this.defaultRegistrationRef == null) {
        this.defaultRegistrationRef = registration;
      }

      return registration;
    });
  }

  public boolean unregisterRegistration(@NonNull ServiceRegistryRegistration<S> registration) {
    return this.executeInWriteLock(() -> {
      var removed = this.registrationsByName.remove(registration.name(), registration);
      if (removed) {
        if (this.registrationsByName.isEmpty()) {
          // there are no more registrations left in this binding, so this binding became obsolete
          // mark this binding as closed and remove the association from the service registry
          this.obsolete = true;
          this.registry.serviceBindings.values().remove(this);
        } else if (registration == this.defaultRegistrationRef) {
          // the removed registration was the default registration previously, so we need to
          // select a new default registration (which is just the first of the other registrations)
          var firstOtherRegistration = this.registrationsByName.firstEntry();
          this.defaultRegistrationRef = firstOtherRegistration.getValue();
        }
      }

      return removed;
    });
  }

  public void unregisterAllByClassLoader(@NonNull ClassLoader classLoader) {
    this.executeInWriteLock(() -> {
      var iterator = this.registrationsByName.values().iterator();
      while (iterator.hasNext()) {
        var serviceInstance = iterator.next().serviceInstance();
        if (serviceInstance.getClass().getClassLoader() == classLoader) {
          iterator.remove();
        }
      }

      return null;
    });
  }

  public void cleanupAndMarkObsolete() {
    this.executeInWriteLock(() -> {
      this.obsolete = true;
      this.registrationsByName.clear();
      return null;
    });
  }

  @UnmodifiableView
  public @NonNull Collection<ServiceRegistryRegistration<S>> registrations() {
    return Collections.unmodifiableCollection(this.registrationsByName.values());
  }

  @UnknownNullability
  private <T> T executeInReadLock(@NonNull Supplier<T> action) {
    this.readLock.lock();
    try {
      return action.get();
    } finally {
      this.readLock.unlock();
    }
  }

  @UnknownNullability
  private <T> T executeInWriteLock(@NonNull Supplier<T> action) {
    this.writeLock.lock();
    try {
      return action.get();
    } finally {
      this.writeLock.unlock();
    }
  }
}
