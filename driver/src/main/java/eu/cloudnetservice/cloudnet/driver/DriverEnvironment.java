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

package eu.cloudnetservice.cloudnet.driver;

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import lombok.NonNull;

/**
 * Represents the current environment in which the driver is running. There are currently two default environments, the
 * node and wrapper which are statically represented in this class.
 * <p>
 * A driver environment can hold additional information in its json properties, these are however never persisted
 * anywhere.
 *
 * @since 4.0
 */
public final class DriverEnvironment extends JsonDocPropertyHolder implements Nameable {

  /**
   * The jvm-static representation of the node environment.
   */
  public static final DriverEnvironment NODE = new DriverEnvironment("node", JsonDocument.newDocument());
  /**
   * The jvm-static representation of the wrapper environment.
   */
  public static final DriverEnvironment WRAPPER = new DriverEnvironment("wrapper", JsonDocument.newDocument());

  private final String name;

  /**
   * Constructs a new driver environment instance.
   *
   * @param name       the name of the environment.
   * @param properties the properties of the environment.
   * @throws NullPointerException if the given name or properties are null.
   */
  public DriverEnvironment(@NonNull String name, @NonNull JsonDocument properties) {
    super(properties);
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }
}
