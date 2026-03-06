/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * A container for a module that was successfully constructed from a module candidate.
 *
 * @since 4.0
 */
public interface ModuleContainer {

  /**
   * Get the current state of the module.
   *
   * @return the current state of the module.
   */
  @NonNull
  ModuleState state();

  /**
   * Get the path from which the module was loaded.
   *
   * @return the path from which the module was loaded.
   */
  @NonNull
  Path source();

  /**
   * Get the parsed module metadata of the module which is contained in the module resource.
   *
   * @return the parsed module metadata of the module.
   */
  @NonNull
  ModuleMetadata metadata();

  /**
   * Get the class loader used for the module.
   *
   * @return the class loader used for the module.
   */
  @NonNull
  ClassLoader classLoader();

  /**
   * The constructed main instance of the module. This instance is singleton for the module and only available after the
   * constructor in the module main entrypoint was invoked.
   *
   * @return the constructed main instance of the module.
   * @throws IllegalStateException if the module is not loaded and therefore no instance being present.
   */
  @NonNull
  Object instance();

  /**
   * Get the injection layer used for the module.
   *
   * @return the injection layer used for the module.
   * @throws IllegalStateException if the module is not loaded and therefore no injection layer being present.
   */
  @NonNull
  InjectionLayer<?> injectionLayer();

  /**
   * Loads this module if this module is in the unloaded state. Otherwise, this method does nothing.
   *
   * @return true if the module was loaded as a result of this method call, false otherwise.
   */
  boolean load();

  /**
   * Reloads this module if this module is in the running state. Otherwise, this method does nothing.
   *
   * @return true if the module was reloaded as a result of this method call, false otherwise.
   */
  boolean reload();

  /**
   * Unloads this module if it is in the running state. Otherwise, this method does nothing.
   *
   * @return true if the module was unloaded as a result of the method call, false otherwise.
   */
  boolean unload();

  /**
   * Removes this module if it is in the unloaded state. If the module is in the running state, it will be unloaded and
   * then removed. A removed module cannot be loaded anymore.
   *
   * @return true if the module was removed as a result of this call, false otherwise.
   */
  boolean remove();
}
