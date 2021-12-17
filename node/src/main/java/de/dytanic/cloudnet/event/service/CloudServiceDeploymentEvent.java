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

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

public final class CloudServiceDeploymentEvent extends CloudServiceEvent implements ICancelable {

  private final TemplateStorage templateStorage;
  private final ServiceDeployment serviceDeployment;

  private volatile boolean cancelled;

  public CloudServiceDeploymentEvent(
    @NotNull ICloudService cloudService,
    @NotNull TemplateStorage templateStorage,
    @NotNull ServiceDeployment serviceDeployment
  ) {
    super(cloudService);

    this.templateStorage = templateStorage;
    this.serviceDeployment = serviceDeployment;
  }

  public @NotNull TemplateStorage storage() {
    return this.templateStorage;
  }

  public @NotNull ServiceDeployment deployment() {
    return this.serviceDeployment;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
