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

import eu.cloudnetservice.driver.document.Document;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a dependency of a module on some external resource, such as a maven dependency. Each external dependency
 * must specify a loader which will resolve the dependency in runtime. If a dependency is marked optional and the
 * loading fails in runtime, the module loading process is not interrupted and the module loading continues.
 *
 * @since 4.0
 */
public interface ModuleExternalDependency {

  /**
   * Get the name of the loader that is responsible for loading this dependency. If the loader cannot be resolved but
   * this dependency is marked optional, the loading process is just skipped.
   *
   * @return the name of the loader that is responsible for loading this dependency
   */
  @NonNull
  String loader();

  /**
   * Get if this dependency if optional, meaning that failures during resolving can safely be ignored.
   *
   * @return true if this dependency if optional, false otherwise.
   */
  boolean optional();

  /**
   * Get the names of the environments that this dependency will be loaded on. Note that there is a special environment
   * called {@code node-wrapper} which indicates that the node should provide the dependency to the wrapper as well.
   *
   * @return the names of the environments that this dependency will be loaded on.
   */
  @NonNull
  @Unmodifiable
  Collection<String> environments();

  /**
   * Get the additional information about this dependency which the loader uses to resolve the external data. This can
   * for example contain a download url or maven dependency coordinates.
   *
   * @return the additional information about this dependency for the loader.
   */
  @NonNull
  Document additionalInformation();
}
