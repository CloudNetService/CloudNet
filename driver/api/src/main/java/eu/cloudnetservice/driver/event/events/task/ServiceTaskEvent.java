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

package eu.cloudnetservice.driver.event.events.task;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceTask;
import lombok.NonNull;

/**
 * Represents an event which is related to service tasks in any way.
 *
 * @since 4.0
 */
public abstract class ServiceTaskEvent extends Event {

  private final ServiceTask task;

  /**
   * Constructs a new service task event.
   *
   * @param task the task associated with this event.
   * @throws NullPointerException if the given task is null.
   */
  public ServiceTaskEvent(@NonNull ServiceTask task) {
    this.task = task;
  }

  /**
   * Get the task which is associated with this event.
   *
   * @return the task which is associated with this event.
   */
  public @NonNull ServiceTask task() {
    return this.task;
  }
}
