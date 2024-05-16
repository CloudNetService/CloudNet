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

import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import lombok.NonNull;

/**
 * This event is being called after a module has been stopped and the tasks with the lifecycle {@link
 * ModuleLifeCycle#STOPPED} of this module have been fired. {@link ModuleWrapper#moduleLifeCycle()} is still {@link
 * ModuleLifeCycle#STARTED} or {@link ModuleLifeCycle#LOADED}.
 *
 * @since 4.0
 */
public final class ModulePostStopEvent extends ModuleEvent {

  /**
   * Constructs a new module post stop event.
   *
   * @param moduleProvider the provider in which the module is loaded.
   * @param module         the module which is associated with this event.
   * @throws NullPointerException if either the provider or wrapper is null.
   */
  public ModulePostStopEvent(@NonNull ModuleProvider moduleProvider, @NonNull ModuleWrapper module) {
    super(moduleProvider, module);
  }
}
