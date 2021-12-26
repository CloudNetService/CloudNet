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

package de.dytanic.cloudnet.event.group;

import de.dytanic.cloudnet.driver.event.Cancelable;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import lombok.NonNull;

public class LocalGroupConfigurationRemoveEvent extends Event implements Cancelable {

  private final GroupConfiguration group;
  private volatile boolean cancelled;

  public LocalGroupConfigurationRemoveEvent(@NonNull GroupConfiguration group) {
    this.group = group;
  }

  public @NonNull GroupConfiguration group() {
    return this.group;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
