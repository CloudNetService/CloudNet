/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called after a dependency for a module has been loaded.
 */
public final class ModulePostInstallDependencyEvent extends UnloadedModuleEvent {

  private final ModuleDependency moduleDependency;

  /**
   * {@inheritDoc}
   *
   * @param dependency the dependency which got loaded.
   */
  public ModulePostInstallDependencyEvent(
    IModuleProvider provider,
    ModuleConfiguration configuration,
    ModuleDependency dependency
  ) {
    super(provider, configuration);
    this.moduleDependency = dependency;
  }

  /**
   * Get the dependency which got loaded for the module.
   *
   * @return the dependency which got loaded for the module.
   */
  public @NotNull ModuleDependency moduleDependency() {
    return this.moduleDependency;
  }
}
