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

package eu.cloudnetservice.driver.module.metadata;

import java.util.Collection;
import lombok.NonNull;

/**
 * Represents a dependency of one module on another module.
 *
 * @param id           the id of the other module being depended on.
 * @param versionRange the range of supported versions of the module, use {@code *} to match all versions.
 * @param environments the driver environments in which this module dependency is applied.
 * @param type         the type of this dependency.
 * @since 4.0
 */
public record ModuleDependency(
  @NonNull String id,
  @NonNull String versionRange,
  @NonNull Collection<String> environments,
  @NonNull DependencyType type
) {

  /**
   * The different possible types of dependencies that modules can have between them.
   *
   * @since 4.0
   */
  public enum DependencyType {

    /**
     * Marks that the other module is required in a matching version for this module to work. If the module is not
     * present in a matching version the module loading process cannot continue.
     */
    REQUIRED,
    /**
     * Marks that the other module is suggested to unlock additional functionality but not a hard requirement.
     */
    SUGGESTED,
    /**
     * Marks that the other module conflicts with the current module in the specified version range. When the other
     * module is present with a matching version the module loading process cannot continue.
     */
    CONFLICTED,
  }
}
