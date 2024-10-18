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

package eu.cloudnetservice.node.event.template;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import lombok.NonNull;

public class ServiceTemplateInstallEvent extends Event {

  private final ServiceTemplate template;
  private final TemplateStorage storage;
  private final ServiceEnvironmentType environmentType;

  public ServiceTemplateInstallEvent(
    @NonNull ServiceTemplate template,
    @NonNull TemplateStorage storage,
    @NonNull ServiceEnvironmentType environmentType
  ) {
    this.template = template;
    this.storage = storage;
    this.environmentType = environmentType;
  }

  public @NonNull ServiceTemplate template() {
    return this.template;
  }

  public @NonNull TemplateStorage storage() {
    return this.storage;
  }

  public @NonNull ServiceEnvironmentType environmentType() {
    return this.environmentType;
  }
}
