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

package eu.cloudnetservice.driver;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Represents the current environment in which the driver is running. There are currently two default environments, the
 * node and wrapper which are statically represented in this class.
 * <p>
 * A driver environment can hold additional information in its json properties, these are however never persisted
 * anywhere.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public final class DriverEnvironment implements DefaultedDocPropertyHolder, Named {

  /**
   * The jvm-static representation of the node environment.
   */
  public static final DriverEnvironment NODE = new DriverEnvironment("node", Document.emptyDocument());
  /**
   * The jvm-static representation of the wrapper environment.
   */
  public static final DriverEnvironment WRAPPER = new DriverEnvironment("wrapper", Document.emptyDocument());

  private final String name;
  private final Document properties;

  /**
   * Constructs a new driver environment instance.
   *
   * @param name       the name of the environment.
   * @param properties the properties of the environment.
   * @throws NullPointerException if the given name or properties are null.
   */
  public DriverEnvironment(@NonNull String name, @NonNull Document properties) {
    this.name = name;
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }
}
