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

package eu.cloudnetservice.node.module.metadata;

import lombok.NonNull;

/**
 * Represents a dependency of one module on another module.
 *
 * @param id           the id of the other module being depended on.
 * @param versionRange the range of supported versions of the module, use {@code *} to match all versions.
 * @param type         the type of this dependency.
 * @since 4.0
 */
public record ModuleDependency(
  @NonNull String id,
  @NonNull String versionRange,
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
     * Marks that the other module is suggested to unlock additional functionality but not a hard requirement. This
     * dependency type will print a notification for the other into the console if the dependency is not present. If no
     * notification is desired use the dependency type {@link #OPTIONAL} instead.
     */
    SUGGESTED,
    /**
     * Marks that the other module is suggested to unlock additional functionality but not a hard requirement. This
     * dependency type will not print a notification into the console about the missing dependency, in contrast to
     * {@link #SUGGESTED}.
     */
    OPTIONAL,
  }
}
