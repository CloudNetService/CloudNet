/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.NonNull;

public abstract class CloudServiceEvent extends DriverEvent {

  private final ICloudService service;

  public CloudServiceEvent(@NonNull ICloudService service) {
    this.service = service;
  }

  public @NonNull ICloudService service() {
    return this.service;
  }

  public @NonNull ServiceConfiguration serviceConfiguration() {
    return this.service.serviceConfiguration();
  }

  public @NonNull ServiceInfoSnapshot serviceInfo() {
    return this.service.serviceInfo();
  }
}
