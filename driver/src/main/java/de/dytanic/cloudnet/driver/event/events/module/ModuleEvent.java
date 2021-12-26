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
import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;
import lombok.NonNull;

/**
 * The {@link ModuleEvent}s are being called for every action in the {@link ModuleProviderHandler}.
 */
public abstract class ModuleEvent extends DriverEvent {

  private final ModuleWrapper module;
  private final ModuleProvider moduleProvider;

  public ModuleEvent(@NonNull ModuleProvider moduleProvider, @NonNull ModuleWrapper module) {
    this.moduleProvider = moduleProvider;
    this.module = module;
  }

  public @NonNull ModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  public @NonNull ModuleWrapper module() {
    return this.module;
  }
}
