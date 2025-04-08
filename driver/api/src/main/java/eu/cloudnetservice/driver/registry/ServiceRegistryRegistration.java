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

import eu.cloudnetservice.driver.base.Named;
import jakarta.inject.Provider;
import lombok.NonNull;

/**
 * A registration for a service in a service registration.
 *
 * @param <S> the type of the service.
 * @since 4.0
 */
public interface ServiceRegistryRegistration<S> extends Provider<S>, Named {

  /**
   * Get the service type that this registration is associated with.
   *
   * @return the service type.
   */
  @NonNull
  Class<S> serviceType();

  /**
   * Get an instance of the service implementation represented by this registration.
   *
   * @return an instance of the service registration represented by this registration.
   */
  @NonNull
  S serviceInstance();

  /**
   * Get if this registration is the default registration for the service.
   *
   * @return true if this registration is the default for the service, false otherwise.
   */
  boolean defaultService();

  /**
   * Marks this registration as the default for the service type.
   *
   * @throws IllegalStateException if this registration is no longer valid.
   */
  void markAsDefaultService();

  /**
   * Get if this registration is still valid, which means that this registration was not unregistered yet.
   *
   * @return true if this registration is still valid, false otherwise.
   */
  boolean valid();

  /**
   * Unregisters this registration.
   *
   * @return true if this registration was unregistered, false otherwise.
   */
  boolean unregister();
}
