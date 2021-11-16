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

package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;

//Generates a constructor with the moduleWrapper as parameter
public final class ExampleOwnEvent extends Event { //Create a own event based of the Event class

  private final IModuleWrapper moduleWrapper;

  public ExampleOwnEvent(IModuleWrapper moduleWrapper) {
    this.moduleWrapper = moduleWrapper;
  }

  public IModuleWrapper getModuleWrapper() {
    return this.moduleWrapper;
  }
}
