/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import lombok.NonNull;

public interface EventManager {

  @NonNull EventManager unregisterListeners(@NonNull ClassLoader classLoader);

  @NonNull EventManager unregisterListener(Object @NonNull ... listeners);

  default @NonNull <T extends Event> T callEvent(@NonNull T event) {
    return this.callEvent("*", event);
  }

  @NonNull <T extends Event> T callEvent(@NonNull String channel, @NonNull T event);

  @NonNull EventManager registerListener(@NonNull Object listener);

  default @NonNull EventManager registerListeners(Object @NonNull ... listeners) {
    for (var listener : listeners) {
      this.registerListener(listener);
    }

    return this;
  }
}
