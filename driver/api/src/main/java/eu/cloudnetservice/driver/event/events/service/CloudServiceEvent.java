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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * Represents an event which is related to services running within the cluster. More specific events are only available
 * on a per-node basis.
 *
 * @since 4.0
 */
public abstract class CloudServiceEvent extends Event {

  protected final ServiceInfoSnapshot serviceInfo;

  /**
   * Constructs a new cloud service event.
   *
   * @param serviceInfo the service info associated with this event.
   * @throws NullPointerException if the given service info is null.
   */
  public CloudServiceEvent(@NonNull ServiceInfoSnapshot serviceInfo) {
    this.serviceInfo = serviceInfo;
  }

  /**
   * Get the service info which is associated with this event.
   *
   * @return the service info which is associated with this event.
   */
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    return this.serviceInfo;
  }
}
