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

import lombok.NonNull;

/**
 * Represents an exception that is thrown when a module depends on another module which is not loaded.
 *
 * @see ModuleDependency
 * @since 4.0
 */
public class ModuleDependencyNotFoundException extends RuntimeException {

  /**
   * Constructs a new instance of this ModuleDependencyNotFoundException.
   *
   * @param dependency      the name of the dependency which is missing.
   * @param requiringModule the module which required the dependency to be present.
   * @throws NullPointerException if dependency or requiringModule is null.
   */
  public ModuleDependencyNotFoundException(@NonNull String dependency, @NonNull String requiringModule) {
    super(String.format("Missing module dependency %s required by %s", dependency, requiringModule));
  }
}
