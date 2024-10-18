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
 * Represents an exception thrown when there are cyclic dependencies between modules
 *
 * @since 4.0
 */
public class ModuleCyclicDependenciesException extends RuntimeException {

  private final String[] dependencyPath;

  public ModuleCyclicDependenciesException(@NonNull String[] dependencyPath) {
    this.dependencyPath = dependencyPath;
  }

  @Override
  public String getMessage() {
    return "Cyclic dependencies detected: " + String.join(" -> ", this.dependencyPath);
  }

  public @NonNull String[] dependencyPath() {
    return this.dependencyPath;
  }
}
