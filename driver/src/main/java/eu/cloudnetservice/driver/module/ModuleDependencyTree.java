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

import eu.cloudnetservice.driver.module.metadata.ModuleMetadata;
import lombok.NonNull;

/**
 * Holds computed information about dependencies between modules. The actual registration of modules into the tree is
 * implementation specific and specifically not exposed into the api.
 *
 * @since 4.0
 */
public interface ModuleDependencyTree {

  /**
   * Checks if the given owner module directly depends on the given other module.
   *
   * @param owner the owner module to check for the dependency.
   * @param other the other module to check for being depended on by the owner module.
   * @return true if the given owner module directly depends on the given other module, false otherwise.
   * @throws NullPointerException if the given owner module or other module is null.
   */
  boolean directlyDependingOn(@NonNull ModuleMetadata owner, @NonNull ModuleMetadata other);

  /**
   * Checks if the given owner module depends on the given other module, either directly or transitively.
   *
   * @param owner the owner module to check for the dependency.
   * @param other the other module to check for being depended on by the owner module.
   * @return true if the given owner module depends on the given other module, false otherwise.
   * @throws NullPointerException if the given owner module or other module is null.
   */
  boolean transitiveDependingOn(@NonNull ModuleMetadata owner, @NonNull ModuleMetadata other);
}
