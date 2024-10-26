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

/**
 * A binding for a single type of service that holds the information about all registrations for the service.
 *
 * @param <S> the model of the service type.
 * @since 4.0
 */
// implementation note: every read and write operation is executed in a lock. this binding
// maintains two lock instances, one for reading and one for writing. this allows many readers
// to read the state at the same time while only write operations need to obtain an exclusive lock.
final class ServiceRegistrationsBinding<S> {

  private final Lock readLock;
  private final Lock writeLock;

  private final Class<S> serviceType;
  private final DefaultServiceRegistry registry;
  private final ServiceRegistryRegistration<S> defaultRegistrationProxy;
  private final SequencedMap<String, ServiceRegistryRegistration<S>> registrationsByName;

  private volatile boolean obsolete;
  private volatile ServiceRegistryRegistration<S> defaultRegistrationRef;

  /**
   * Constructs a new service registration binding.
   *
   * @param serviceType     the type of the service that is managed by this binding.
   * @param serviceRegistry the service registry in which this binding is registered.
   * @throws NullPointerException if the given service type or service registry is null.
   */
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

  /**
   * Get the proxy service registration which always delegates to the current default service registration.
   *
   * @return the proxy service registration for the default service.
   */
  public @NonNull ServiceRegistryRegistration<S> defaultRegistrationProxy() {
    return this.defaultRegistrationProxy;
  }

  /**
   * Get if this registration binding is still valid.
   *
   * @return true if this registration is still valid, false otherwise.
   */
  public boolean valid() {
    return this.executeInReadLock(() -> !this.obsolete);
  }

  /**
   * Checks if the given registration is still valid in this binding.
   *
   * @param registration the registration to check.
   * @return true if the given registration is still valid, false otherwise.
   * @throws NullPointerException if the given registration is null.
   */
  public boolean registrationValid(@NonNull ServiceRegistryRegistration<S> registration) {
    return this.executeInReadLock(() -> !this.obsolete && this.registrationsByName.containsValue(registration));
  }

  /**
   * Checks if the given registration is the default registration for the service.
   *
   * @param registration the registration to check.
   * @return true if the given registration is the default service registration, false otherwise.
   * @throws NullPointerException if the given registration is null.
   */
  public boolean registrationIsDefault(@NonNull ServiceRegistryRegistration<S> registration) {
    return this.executeInReadLock(() -> !this.obsolete && this.defaultRegistrationRef == registration);
  }

  /**
   * Marks the given registration as the default registration.
   *
   * @param registration the registration to mark as the default registration.
   * @throws NullPointerException  if the given registration is null.
   * @throws IllegalStateException if the given registration is not part of this binding.
   */
  public void markAsDefaultRegistration(@NonNull ServiceRegistryRegistration<S> registration) {
    this.executeInWriteLock(() -> {
      Preconditions.checkState(this.registrationsByName.containsValue(registration), "registration no longer valid");
      this.defaultRegistrationRef = registration;
      return null;
    });
  }

  /**
   * Get the service registration that is registered for the given name.
   *
   * @param serviceName the name of the service registration to get.
   * @return the service registration associated with the given name, null if no such registration exists.
   * @throws NullPointerException if the given name is null.
   */
  public @Nullable ServiceRegistryRegistration<S> findRegistrationByName(@NonNull String serviceName) {
    return this.executeInReadLock(() -> this.registrationsByName.get(serviceName));
  }

  /**
   * Registers a singleton service into this binding. If a binding with the same name already exists, the old binding is
   * returned instead.
   *
   * @param serviceName           the name to associate the service registration with.
   * @param serviceImplementation the implementation of the service to register.
   * @return a registration representing the service mapping.
   * @throws NullPointerException if the given service name or service implementation is null.
   */
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

  /**
   * Registers a constructing service into this binding, which is a service that returns a new instance of the service
   * type on each invocation. If a binding with the same name already exists, the old binding is returned instead.
   *
   * @param serviceName        the name to associate the service registration with.
   * @param implementationType the type that implements the managed service type.
   * @return a registration representing the service mapping.
   * @throws NullPointerException     if the given service name or service implementation type is null.
   * @throws IllegalArgumentException if the implementation type has no or an inaccessible no-args constructor.
   */
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
      () -> this.registry.registerConstructingProvider(this.serviceType, serviceName, implementationType));
  }

  /**
   * Registers a new service registration into this binding. If the binding is the first registration, the default
   * service will be set to that registration as well.
   *
   * @param serviceName         the name to associate the new service registration with.
   * @param registrationFactory the factory to use to construct the registration if needed.
   * @return a new registration if the service was registered successfully, null if this binding became obsolete.
   * @throws NullPointerException if the given service name or registration factory is null.
   */
  private @Nullable ServiceRegistryRegistration<S> register(
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

  /**
   * Unregisters the given registration from this binding, marking this binding as obsolete if no more registrations
   * remain after this call. If the given registration was the default registration, the first of the remaining
   * registrations will be promoted to the default registration.
   *
   * @param registration the registration to unregister from this binding.
   * @return true if the registration was unregistered from this binding, false otherwise.
   * @throws NullPointerException if the given registration is null.
   */
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

  /**
   * Unregisters all service registrations which uses an implementation type that was loaded by the given class loader.
   *
   * @param classLoader the class loader of which all associated registrations should be removed.
   * @throws NullPointerException if the given class loader is null.
   */
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

  /**
   * Removes all service registration that are stored in this binding and marks this binding as obsolete.
   */
  public void cleanupAndMarkObsolete() {
    this.executeInWriteLock(() -> {
      this.obsolete = true;
      this.registrationsByName.clear();
      return null;
    });
  }

  /**
   * Get an unmodifiable view of all registrations that are registered in this binding.
   *
   * @return an unmodifiable view of all registrations that are registered in this binding.
   */
  @UnmodifiableView
  public @NonNull Collection<ServiceRegistryRegistration<S>> registrations() {
    return Collections.unmodifiableCollection(this.registrationsByName.values());
  }

  /**
   * Executes the given action in the read lock of this binding. The read lock allows for multiple threads to read at
   * the same time, while no write operations in a write lock can happen. Write operations should not be executed within
   * this lock.
   *
   * @param action the action to execute once the read lock of this binding is acquired.
   * @param <T>    the type that is returned by the action supplier.
   * @return the result of the given action.
   * @throws NullPointerException if the given action is null.
   */
  @UnknownNullability
  private <T> T executeInReadLock(@NonNull Supplier<T> action) {
    this.readLock.lock();
    try {
      return action.get();
    } finally {
      this.readLock.unlock();
    }
  }

  /**
   * Executes the given action in the write lock of this binding. Once the write lock is obtained for the action, no
   * other thread can obtain a read or write lock. Write and read operations can be performed safely within the lock.
   *
   * @param action the action to execute once the write lock of this binding is acquired.
   * @param <T>    the type that is returned by the action supplier.
   * @return the result of the given action.
   * @throws NullPointerException if the given action is null.
   */
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
