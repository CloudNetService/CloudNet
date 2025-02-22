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

package eu.cloudnetservice.driver.event.events.service;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * An event being fired when a service updates its information. Normally an update is triggered by one of:
 * <ol>
 *   <li>a specific call to the wrapper api by a plugin.
 *   <li>a state change of the service, for example from running to deleted.
 *   <li>a change happening on the service, for example a player join or leave.
 * </ol>
 * <p>
 * This event contains the service info after the update of it. To save resources (and as there is no need internally
 * for it) the old service info will not be available in this event and must rather be read from another cache (for
 * example an own one).
 *
 * @since 4.0
 */
public final class CloudServiceUpdateEvent extends CloudServiceEvent {

  /**
   * Constructs a new cloud service update event.
   *
   * @param serviceInfo the service info associated with this event.
   * @throws NullPointerException if the given service info is null.
   */
  public CloudServiceUpdateEvent(@NonNull ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}
