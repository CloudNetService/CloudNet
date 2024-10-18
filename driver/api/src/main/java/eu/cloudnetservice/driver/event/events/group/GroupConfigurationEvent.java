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

package eu.cloudnetservice.driver.event.events.group;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import lombok.NonNull;

/**
 * Represents an event which is related group configuration handling in any way.
 *
 * @since 4.0
 */
public abstract class GroupConfigurationEvent extends Event {

  private final GroupConfiguration configuration;

  /**
   * Constructs a new group configuration event.
   *
   * @param configuration the configuration associated with this event.
   * @throws NullPointerException if the given configuration is null.
   */
  public GroupConfigurationEvent(@NonNull GroupConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the group configuration associated with this event.
   *
   * @return the group configuration associated with this event.
   */
  public @NonNull GroupConfiguration configuration() {
    return this.configuration;
  }
}
