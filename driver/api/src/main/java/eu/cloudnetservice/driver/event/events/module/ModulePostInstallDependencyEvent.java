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

package eu.cloudnetservice.driver.event.events.module;

import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleProvider;
import lombok.NonNull;

/**
 * This event gets called after a dependency for a module has been loaded.
 *
 * @since 4.0
 */
public final class ModulePostInstallDependencyEvent extends UnloadedModuleEvent {

  private final ModuleDependency moduleDependency;

  /**
   * Creates a new module installed dependency (post) event.
   *
   * @param provider      the module provider by which the module got loaded.
   * @param configuration the configuration of the module which got loaded as there is no instance yet created.
   * @param dependency    the dependency which got loaded.
   * @throws NullPointerException if the given provider, configuration or dependency is null.
   */
  public ModulePostInstallDependencyEvent(
    @NonNull ModuleProvider provider,
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) {
    super(provider, configuration);
    this.moduleDependency = dependency;
  }

  /**
   * Get the dependency which got loaded for the module.
   *
   * @return the dependency which got loaded for the module.
   */
  public @NonNull ModuleDependency moduleDependency() {
    return this.moduleDependency;
  }
}
