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

import com.google.common.base.Preconditions;

public interface IEventManager {

  IEventManager registerListener(Object listener);

  IEventManager unregisterListener(Object listener);

  IEventManager unregisterListener(Class<?> listener);

  IEventManager unregisterListeners(ClassLoader classLoader);

  IEventManager unregisterListeners(Object... listeners);

  IEventManager unregisterListeners(Class<?>... classes);

  IEventManager unregisterAll();

  <T extends Event> T callEvent(String channel, T event);

  default <T extends Event> T callEvent(T event) {
    return this.callEvent("*", event);
  }

  default IEventManager registerListeners(Object... listeners) {
    Preconditions.checkNotNull(listeners);

    for (Object listener : listeners) {
      this.registerListener(listener);
    }

    return this;
  }
}
