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

import eu.cloudnetservice.driver.service.ServiceTask;
import lombok.NonNull;

/**
 * An event being fired when a service task gets removed.
 *
 * @since 4.0
 */
public final class ServiceTaskRemoveEvent extends ServiceTaskEvent {

  /**
   * Constructs a new service task remove event.
   *
   * @param task the task associated with this event.
   * @throws NullPointerException if the given task is null.
   */
  public ServiceTaskRemoveEvent(@NonNull ServiceTask task) {
    super(task);
  }
}
