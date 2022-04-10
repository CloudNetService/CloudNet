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

package eu.cloudnetservice.wrapper.event.service;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.events.service.CloudServiceEvent;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * The event is called when a new ServiceInfoSnapshot has been created to update this service. With the getProperties()
 * Method by ServiceInfoSnapshot you can added optional properties.
 * <p>
 * This Event will called every update with the Wrapper API
 *
 * @see ServiceInfoSnapshot
 * @see Event
 */
public class ServiceInfoSnapshotConfigureEvent extends CloudServiceEvent {

  public ServiceInfoSnapshotConfigureEvent(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    super(serviceInfoSnapshot);
  }
}
