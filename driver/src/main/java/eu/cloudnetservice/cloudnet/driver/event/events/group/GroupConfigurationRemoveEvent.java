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

package eu.cloudnetservice.cloudnet.driver.event.events.group;

import eu.cloudnetservice.cloudnet.driver.event.Event;
import eu.cloudnetservice.cloudnet.driver.service.GroupConfiguration;
import lombok.NonNull;

/**
 * Event triggered when a group configuration was removed.
 *
 * @since 4.0
 */
public class GroupConfigurationRemoveEvent extends Event {

  private final GroupConfiguration configuration;

  /**
   * Constructs a new group configuration remove event with the given configuration as its base.
   *
   * @param configuration the configuration which was removed.
   * @throws NullPointerException if the given configuration is null.
   */
  public GroupConfigurationRemoveEvent(@NonNull GroupConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the configuration which was removed.
   *
   * @return the configuration which was removed.
   */
  public @NonNull GroupConfiguration configuration() {
    return this.configuration;
  }
}
