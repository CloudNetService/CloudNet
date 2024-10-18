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

package eu.cloudnetservice.node.event.service;

import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;

public final class CloudServiceTemplateLoadEvent extends CloudServiceEvent implements Cancelable {

  private final TemplateStorage storage;
  private final ServiceTemplate template;

  private volatile boolean cancelled;

  public CloudServiceTemplateLoadEvent(
    @NonNull CloudService cloudService,
    @NonNull TemplateStorage storage,
    @NonNull ServiceTemplate template
  ) {
    super(cloudService);

    this.storage = storage;
    this.template = template;
  }

  public @NonNull TemplateStorage storage() {
    return this.storage;
  }

  public @NonNull ServiceTemplate template() {
    return this.template;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
