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
import org.jetbrains.annotations.NotNull;

public abstract class CloudServiceEvent extends DriverEvent {

  private final ICloudService service;

  public CloudServiceEvent(@NotNull ICloudService service) {
    this.service = service;
  }

  public @NotNull ICloudService getService() {
    return this.service;
  }

  public @NotNull ServiceConfiguration getConfiguration() {
    return this.service.getServiceConfiguration();
  }

  public @NotNull ServiceInfoSnapshot getServiceInfo() {
    return this.service.getServiceInfoSnapshot();
  }
}
