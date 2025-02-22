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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.base.Named;
import lombok.NonNull;

/**
 * Represents an environment a service can run in. The environment for example decides which application file gets
 * chosen when starting a service. Environments are grouped in a service environment type and both must be registered on
 * a node which picks up a service using the environment.
 *
 * @since 4.0
 */
public class ServiceEnvironment implements Named, Cloneable {

  private final String name;
  private final String environmentType;

  /**
   * Constructs a new service environment instance.
   *
   * @param name            the name of the environment.
   * @param environmentType the environment type which is associated with this environment.
   * @throws NullPointerException if the given name or environment type is null.
   */
  protected ServiceEnvironment(@NonNull String name, @NonNull String environmentType) {
    this.name = name;
    this.environmentType = environmentType;
  }

  /**
   * Constructs a new builder instance for a service environment.
   *
   * @return a service environment builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for a service environment, copying the properties of the given environment into
   * the builder first.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in an environment which
   * is equal but not the same as the given one.
   *
   * @param environment the environment to copy the properties from.
   * @return a new builder for a service environment holding the same properties as the given environment.
   * @throws NullPointerException if the given environment is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceEnvironment environment) {
    return builder().name(environment.name()).environmentType(environment.environmentType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the name of the environment type which is associated with this environment.
   *
   * @return the name of the associated environment type.
   */
  public @NonNull String environmentType() {
    return this.environmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceEnvironment clone() {
    try {
      return (ServiceEnvironment) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * A builder for a service environment.
   *
   * @since 4.0
   */
  public static class Builder {

    private String name;
    private String environmentType;

    /**
     * Sets the name of the environment created by this builder.
     *
     * @param name the name of the environment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the name of the environment type which is associated with this environment. The environment type holds the
     * information which are shared between all environments grouped by it, for example which configuration files need
     * to be edited to start the service.
     *
     * @param environmentType the name of the environment type which is associated with this environment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment type name is null.
     */
    public @NonNull Builder environmentType(@NonNull String environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    /**
     * Sets the name of the environment type which is associated with this environment. The environment type holds the
     * information which are shared between all environments grouped by it, for example which configuration files need
     * to be edited to start the service.
     *
     * @param type the environment type which should be associated with this environment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment type is null.
     */
    public @NonNull Builder environmentType(@NonNull ServiceEnvironmentType type) {
      this.environmentType = type.name();
      return this;
    }

    /**
     * Builds a service environment instance based on this builder.
     *
     * @return the created service environment.
     * @throws NullPointerException if no name or parent environment type was given.
     */
    public @NonNull ServiceEnvironment build() {
      Preconditions.checkNotNull(this.name, "no name given");
      Preconditions.checkNotNull(this.environmentType, "no environment type given");

      return new ServiceEnvironment(this.name, this.environmentType);
    }
  }
}
