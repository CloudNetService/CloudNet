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
 * Represents an exception that is thrown if a module depends on a specific version of a module and the versions do not
 * match.
 *
 * @see ModuleDependency
 * @since 4.0
 */
public class ModuleDependencyOutdatedException extends RuntimeException {

  /**
   * Creates a new instance of this ModuleDependencyOutdatedException.
   *
   * @param requiringModule the module which requires the dependency.
   * @param dependency      the dependency which is outdated.
   * @param semverIndex     the semver index name: major, minor, patch
   * @param required        the required version of the semver index.
   * @param actual          the actual running version of the semver index.
   * @throws NullPointerException if requiringModule, dependency or semverIndex is null.
   */
  public ModuleDependencyOutdatedException(
    @NonNull ModuleWrapper requiringModule,
    @NonNull ModuleDependency dependency,
    @NonNull String semverIndex,
    int required,
    int actual
  ) {
    super(String.format(
      "Module %s:%s requires minimum %s version %d of %s:%s but is currently %d",
      requiringModule.module().group(), requiringModule.module().name(),
      semverIndex,
      required,
      dependency.group(), dependency.name(),
      actual
    ));
  }
}
