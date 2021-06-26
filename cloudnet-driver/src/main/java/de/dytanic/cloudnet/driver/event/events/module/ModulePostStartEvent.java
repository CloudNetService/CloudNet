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
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;

/**
 * This event is being called after a module has been started and the tasks with the lifecycle {@link
 * ModuleLifeCycle#STARTED} of this module have been fired. {@link IModuleWrapper#getModuleLifeCycle()} is still {@link
 * ModuleLifeCycle#LOADED} or {@link ModuleLifeCycle#STOPPED}.
 */
public final class ModulePostStartEvent extends ModuleEvent {

  public ModulePostStartEvent(IModuleProvider moduleProvider, IModuleWrapper module) {
    super(moduleProvider, module);
  }
}
