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

import eu.cloudnetservice.driver.document.property.DocPropertyHolder;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependency;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The metadata of the module which contains all the information needed to actually load it.
 *
 * @since 4.0
 */
public interface ModuleMetadata extends DocPropertyHolder {

  /**
   * Get the id of the module, which must match the pattern {@code ^[a-z][a-z0-9-.]{4,63}$}.
   *
   * @return the id of the mod.
   */
  @NonNull
  String id();

  /**
   * Get the display name of the module. Cannot be blank.
   *
   * @return the display name of the module.
   */
  @NonNull
  String displayName();

  /**
   * Get the description of the module. Cannot be blank.
   *
   * @return the description of the module.
   */
  @NonNull
  String description();

  /**
   * Get the main entrypoint of the module, can for example be a binary class name for java. Cannot be blank.
   *
   * @return the main entrypoint of the module.
   */
  @NonNull
  String entrypoint();

  /**
   * Get the parsed version of the module.
   *
   * @return the parsed version of the module.
   */
  @NonNull
  ModuleVersion version();

  /**
   * Get the names of the licenses used for this module. It's recommended to use SPDX license identifiers for open
   * source licenses, but it can really contain any name of a license.
   *
   * @return the names of the licenses used for this module.
   */
  @NonNull
  @Unmodifiable
  Collection<String> licenses();

  /**
   * Get the artifacts that are managed by this module.
   *
   * @return the artifacts that are managed by this module.
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleArtifact> artifacts();

  /**
   * Get the dependencies on other modules that this module has.
   *
   * @return the dependencies on other modules that this module has.
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleDependency> moduleDependencies();

  /**
   * Get the external dependencies that this module has.
   *
   * @return the external dependencies that this module has.
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleExternalDependency> externalDependencies();

  /**
   * Get the main authors of this module.
   *
   * @return the main authors of this module.
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleContributor> authors();

  /**
   * Get the contributors of this module. There is no requirement to add every person that contributed to this module
   * into this collection.
   *
   * @return the contributors of this module.
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleContributor> contributors();
}
