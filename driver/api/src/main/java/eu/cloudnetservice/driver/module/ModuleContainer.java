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

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.locator.ModuleResource;
import eu.cloudnetservice.driver.module.metadata.ModuleMetadata;
import lombok.NonNull;

/**
 * A container for a module that was successfully constructed from a module candidate.
 *
 * @since 4.0
 */
public interface ModuleContainer {

  /**
   * The constructed main instance of the module. This instance is singleton for the module and only available after the
   * constructor in the module main entrypoint was invoked.
   *
   * @return the constructed main instance of the module.
   */
  @NonNull
  Object instance();

  /**
   * Get the module resource from which the module was loaded.
   *
   * @return the module resource from which the module was loaded.
   */
  @NonNull
  ModuleResource resource();

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
   * Get the injection layer used for the module.
   *
   * @return the injection layer used for the module.
   */
  @NonNull
  InjectionLayer<?> injectionLayer();
}
