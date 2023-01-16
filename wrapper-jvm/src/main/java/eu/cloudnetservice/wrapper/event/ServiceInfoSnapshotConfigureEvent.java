/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.event;

import eu.cloudnetservice.driver.event.events.service.CloudServiceEvent;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * An event which is called when a service snapshot which is represented by the current wrapper is updated. Listeners of
 * this event can change the service snapshot which is sent into the cluster as needed (for example appending custom
 * properties to it).
 *
 * @since 4.0
 */
public final class ServiceInfoSnapshotConfigureEvent extends CloudServiceEvent {

  /**
   * Constructs a new ServiceInfoSnapshotConfigureEvent instance.
   *
   * @param serviceInfoSnapshot the service snapshot which is being configured.
   * @throws NullPointerException if the given service snapshot is null.
   */
  public ServiceInfoSnapshotConfigureEvent(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    super(serviceInfoSnapshot);
  }
}
