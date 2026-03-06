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

package eu.cloudnetservice.node.module.dependency;

import eu.cloudnetservice.driver.document.property.DocPropertyHolder;
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
public interface ModuleExternalDependency extends DocPropertyHolder {

  /**
   * Get the name of the loader that is responsible for loading this dependency. If the loader cannot be resolved but
   * this dependency is marked optional, the loading process is just skipped.
   *
   * @return the name of the loader that is responsible for loading this dependency
   */
  @NonNull
  String loader();

  /**
   * Get if this dependency is optional, meaning that failures during resolving can safely be ignored.
   *
   * @return true if this dependency is optional, false otherwise.
   */
  boolean optional();

  /**
   * Get the names of the service environments that this dependency will be loaded on. There are three special
   * environments that can be used as well:
   * <ol>
   *   <li>{@code all}. Indicates that the dependency should be loaded on all environments.
   *   <li>{@code node}. Indicates that the dependency should be loaded on the node.
   *   <li>{@code wrapper}. Indicates that the dependency should be loaded on all services.
   * </ol>
   *
   * @return the names of the environments that this dependency will be loaded on.
   */
  @NonNull
  @Unmodifiable
  Collection<String> environments();
}
