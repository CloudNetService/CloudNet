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

package de.dytanic.cloudnet.driver.event.invoker;

import de.dytanic.cloudnet.driver.event.Event;

/**
 * Responsible for invoking event listener methods without reflection. An implementation is automatically generated for
 * every event listener method when registered.
 */
@FunctionalInterface
public interface ListenerInvoker {

  /**
   * Invokes the event listener method.
   *
   * @param event The event the listener method should be invoked with. Passing an event with the wrong type will result
   *              in an exception
   */
  void invoke(Event event);
}
