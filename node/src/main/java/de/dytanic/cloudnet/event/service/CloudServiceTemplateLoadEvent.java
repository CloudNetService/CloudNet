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
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

public final class CloudServiceTemplateLoadEvent extends CloudServiceEvent implements ICancelable {

  private final TemplateStorage storage;
  private final ServiceTemplate template;

  private volatile boolean cancelled;

  public CloudServiceTemplateLoadEvent(
    @NotNull ICloudService cloudService,
    @NotNull TemplateStorage storage,
    @NotNull ServiceTemplate template
  ) {
    super(cloudService);

    this.storage = storage;
    this.template = template;
  }

  public @NotNull TemplateStorage storage() {
    return this.storage;
  }

  public @NotNull ServiceTemplate template() {
    return this.template;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
