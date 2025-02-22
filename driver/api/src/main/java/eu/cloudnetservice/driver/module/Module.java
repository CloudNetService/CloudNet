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

package eu.cloudnetservice.driver.module;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the main class or entry point of the module. The entry point is allowed to specify own module lifecycle
 * update methods using the module task annotation.
 *
 * @see DriverModule
 * @see DefaultModule
 * @see ModuleTaskEntry
 * @see ModuleTask
 */
public interface Module extends Named {

  /**
   * Initializes the module with the necessary information. This method can only be called once.
   *
   * @param loader  the class loader used to load all dependencies and the main class of the module.
   * @param wrapper the created module wrapper which wraps this module.
   * @param config  the deserialized module configuration located in the module file.
   * @throws IllegalArgumentException if this module instance is already initialized.
   * @throws NullPointerException     if the provided loader, wrapper or module config is null.
   */
  void init(@NonNull ClassLoader loader, @NonNull ModuleWrapper wrapper, @NonNull ModuleConfiguration config);

  /**
   * Get the module wrapper which is associated with this module.
   *
   * @return the module wrapper which is associated with this module.
   */
  @NonNull ModuleWrapper moduleWrapper();

  /**
   * Get the class loader which is responsible for this module.
   *
   * @return the class loader which is responsible for this module.
   */
  @NonNull ClassLoader classLoader();

  /**
   * Get the module configuration which was deserialized based on the information located in the module.
   *
   * @return the module configuration located in this module file.
   */
  @NonNull ModuleConfiguration moduleConfig();

  /**
   * Get the group of this module.
   *
   * @return the group id of this module.
   */
  default @NonNull String group() {
    return this.moduleConfig().group();
  }

  /**
   * Get the name of this module.
   *
   * @return the name of this module.
   */
  @Override
  default @NonNull String name() {
    return this.moduleConfig().name();
  }

  /**
   * Get the version of this module.
   *
   * @return the version of this module.
   */
  default @NonNull String version() {
    return this.moduleConfig().version();
  }

  /**
   * Get the website of this module.
   *
   * @return the website of this module.
   */
  default @Nullable String website() {
    return this.moduleConfig().website();
  }

  /**
   * Get the description of this module.
   *
   * @return the description of this module.
   */
  default @Nullable String description() {
    return this.moduleConfig().description();
  }
}
