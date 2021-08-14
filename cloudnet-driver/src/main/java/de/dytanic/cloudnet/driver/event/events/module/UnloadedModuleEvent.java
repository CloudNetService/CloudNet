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

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Defines events which will be called when a module is not yet instantiated.
 */
public abstract class UnloadedModuleEvent extends DriverEvent {

  private final IModuleProvider moduleProvider;
  private final ModuleConfiguration moduleConfiguration;

  /**
   * Creates a new unloaded module event instance.
   *
   * @param moduleProvider      the module provider by which the module got loaded.
   * @param moduleConfiguration the configuration of the module which got loaded as there is no instance yet created.
   */
  public UnloadedModuleEvent(IModuleProvider moduleProvider, ModuleConfiguration moduleConfiguration) {
    this.moduleProvider = moduleProvider;
    this.moduleConfiguration = moduleConfiguration;
  }

  /**
   * Get the module provider which loaded the module.
   *
   * @return the module provider which loaded the module.
   */
  public @NotNull IModuleProvider getModuleProvider() {
    return this.moduleProvider;
  }

  /**
   * Get the module configuration of the module this event is associated with.
   *
   * @return the module configuration of the module.
   */
  public @NotNull ModuleConfiguration getModuleConfiguration() {
    return this.moduleConfiguration;
  }
}
