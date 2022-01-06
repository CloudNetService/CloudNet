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
 * Event called when a group configuration was added or updated. This is due to the fact that adding or updating makes
 * no difference for the cloudnet internal handling so there is no reason to split the same behaviour into two events.
 *
 * @since 4.0
 */
public class GroupConfigurationAddEvent extends Event {

  private final GroupConfiguration configuration;

  /**
   * Creates a new group configuration add event with the given configuration.
   *
   * @param configuration the configuration which was added or updated.
   * @throws NullPointerException if the given configuration is null.
   */
  public GroupConfigurationAddEvent(@NonNull GroupConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the group configuration which was added or updated and triggered this event.
   *
   * @return the group configuration which was added or updated.
   */
  public @NonNull GroupConfiguration configuration() {
    return configuration;
  }
}
