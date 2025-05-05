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

import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import java.lang.invoke.MethodHandle;
import lombok.NonNull;

/**
 * A service registration which obtains a new instance of the service implementation on each invocation.
 *
 * @param serviceType       the type of the service that is implemented by this registration.
 * @param serviceName       the name of the service registration.
 * @param constructorHandle the method handle of the constructor to call to obtain a new service instance.
 * @param binding           the binding in which this registration is registered.
 * @param <S>               the type modeling the implemented service.
 * @since 4.0
 */
record NewInstanceServiceRegistration<S>(
  @NonNull Class<S> serviceType,
  @NonNull String serviceName,
  @NonNull MethodHandle constructorHandle,
  @NonNull ServiceRegistrationsBinding<S> binding
) implements ServiceRegistryRegistration<S> {

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull S serviceInstance() {
    try {
      return (S) this.constructorHandle.invokeExact();
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to retrieve service instance", throwable);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean defaultService() {
    return this.binding.registrationIsDefault(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void markAsDefaultService() {
    this.binding.markAsDefaultRegistration(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean valid() {
    return this.binding.registrationValid(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregister() {
    return this.binding.unregisterRegistration(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull S get() {
    return this.serviceInstance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.serviceName;
  }
}
