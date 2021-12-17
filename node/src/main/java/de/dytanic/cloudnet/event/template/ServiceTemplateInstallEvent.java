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

package de.dytanic.cloudnet.event.template;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import org.jetbrains.annotations.NotNull;

public class ServiceTemplateInstallEvent extends Event {

  private final ServiceTemplate template;
  private final SpecificTemplateStorage storage;
  private final ServiceEnvironmentType environmentType;

  public ServiceTemplateInstallEvent(
    @NotNull ServiceTemplate template,
    @NotNull SpecificTemplateStorage storage,
    @NotNull ServiceEnvironmentType environmentType
  ) {
    this.template = template;
    this.storage = storage;
    this.environmentType = environmentType;
  }

  public @NotNull ServiceTemplate template() {
    return this.template;
  }

  public @NotNull SpecificTemplateStorage storage() {
    return this.storage;
  }

  public @NotNull ServiceEnvironmentType environmentType() {
    return this.environmentType;
  }
}
