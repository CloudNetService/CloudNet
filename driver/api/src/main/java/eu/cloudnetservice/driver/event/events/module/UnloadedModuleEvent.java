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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.module.ModuleProvider;
import lombok.NonNull;

/**
 * Defines events which will be called when a module is not yet instantiated.
 *
 * @since 4.0
 */
public abstract class UnloadedModuleEvent extends Event {

  private final ModuleProvider moduleProvider;
  private final ModuleConfiguration moduleConfiguration;

  /**
   * Creates a new unloaded module event instance.
   *
   * @param moduleProvider      the module provider by which the module got loaded.
   * @param moduleConfiguration the configuration of the module which got loaded as there is no instance yet created.
   */
  public UnloadedModuleEvent(
    @NonNull ModuleProvider moduleProvider,
    @NonNull ModuleConfiguration moduleConfiguration
  ) {
    this.moduleProvider = moduleProvider;
    this.moduleConfiguration = moduleConfiguration;
  }

  /**
   * Get the module provider which loaded the module.
   *
   * @return the module provider which loaded the module.
   */
  public @NonNull ModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  /**
   * Get the module configuration of the module this event is associated with.
   *
   * @return the module configuration of the module.
   */
  public @NonNull ModuleConfiguration moduleConfiguration() {
    return this.moduleConfiguration;
  }
}
