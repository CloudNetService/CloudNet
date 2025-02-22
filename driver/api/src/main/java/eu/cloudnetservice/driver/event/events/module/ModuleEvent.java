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
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import lombok.NonNull;

/**
 * The base event for all events which are related to modules. By default, all module state changes have their own
 * event. See the package files for a list of all modules.
 * <p>
 * Each module event contains the associated module and the provider which is associated with the module action.
 *
 * @since 4.0
 */
public abstract class ModuleEvent extends Event {

  private final ModuleWrapper module;
  private final ModuleProvider moduleProvider;

  /**
   * Constructs a new module event.
   *
   * @param moduleProvider the provider in which the module is loaded.
   * @param module         the module which is associated with this event.
   * @throws NullPointerException if either the provider or wrapper is null.
   */
  public ModuleEvent(@NonNull ModuleProvider moduleProvider, @NonNull ModuleWrapper module) {
    this.moduleProvider = moduleProvider;
    this.module = module;
  }

  /**
   * Get the module provider from which the associated module was loaded.
   *
   * @return the module provider from which the associated module was loaded.
   */
  public @NonNull ModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  /**
   * Get the module on which the action is currently taking place.
   *
   * @return the module on which the action is currently taking place.
   */
  public @NonNull ModuleWrapper module() {
    return this.module;
  }
}
