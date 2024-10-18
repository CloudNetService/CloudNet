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

import eu.cloudnetservice.driver.service.GroupConfiguration;
import lombok.NonNull;

/**
 * Event called when a group configuration was added or updated. This is due to the fact that adding or updating makes
 * no difference for the cloudnet internal handling so there is no reason to split the same behaviour into two events.
 *
 * @since 4.0
 */
public final class GroupConfigurationAddEvent extends GroupConfigurationEvent {

  /**
   * Constructs a new group configuration add event.
   *
   * @param configuration the configuration associated with this event.
   * @throws NullPointerException if the given configuration is null.
   */
  public GroupConfigurationAddEvent(@NonNull GroupConfiguration configuration) {
    super(configuration);
  }
}
