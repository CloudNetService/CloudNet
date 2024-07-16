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

package eu.cloudnetservice.driver.module.dependency;

import eu.cloudnetservice.common.Named;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * A loader for external module dependencies.
 *
 * @since 4.0
 */
public interface ModuleExternalDependencyLoader extends Named {

  /**
   * Loads the given external dependency as a sub-file into the given cache path.
   *
   * @param cachePath  the cache patch where the loaded dependency should be stored.
   * @param dependency the dependency which should be loaded.
   * @return the path to the loaded external dependency, must be relative to the given cache path.
   * @throws NullPointerException if the given cache path or external dependency is null.
   * @throws Exception            if any exception occurs during the external dependency loading.
   */
  @NonNull
  Path loadExternalDependency(@NonNull Path cachePath, @NonNull ModuleExternalDependency dependency) throws Exception;
}
