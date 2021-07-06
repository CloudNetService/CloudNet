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

package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.driver.event.invoker.ListenerInvoker;

public interface IRegisteredEventListener extends Comparable<IRegisteredEventListener> {

  void fireEvent(Event event);

  EventListener getEventListener();

  EventPriority getPriority();

  Object getInstance();

  ListenerInvoker getInvoker();

  Class<?> getEventClass();

  String getMethodName();

  @Override
  default int compareTo(IRegisteredEventListener other) {
    return this.getPriority().compareTo(other.getPriority());
  }
}
