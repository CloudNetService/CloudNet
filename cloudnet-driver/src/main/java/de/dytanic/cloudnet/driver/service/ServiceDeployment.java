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

package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Defines the properties for a service that is copied to a specified template. It contains the {@link ServiceTemplate}
 * and {@code excludes} for excluded files/directories. For excluded files, just add the path to the file to the
 * excludes (e.g. "spigot.jar"). For excluded directories, the path has to end with a "/" (e.g. "plugins/").
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceDeployment extends BasicJsonDocPropertyable {

  private final ServiceTemplate template;
  private final Collection<String> excludes;

  public ServiceDeployment(ServiceTemplate template, Collection<String> excludes) {
    this(template, excludes, JsonDocument.newDocument());
  }

  public ServiceDeployment(ServiceTemplate template, Collection<String> excludes, JsonDocument properties) {
    this.template = template;
    this.excludes = excludes;
    this.properties = properties;
  }

  public ServiceTemplate getTemplate() {
    return this.template;
  }

  public Collection<String> getExcludes() {
    return this.excludes;
  }
}
