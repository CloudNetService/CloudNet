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

import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends JsonDocPropertyHolder {

  protected Collection<ServiceTemplate> templates;
  protected Collection<ServiceDeployment> deployments;
  protected Collection<ServiceRemoteInclusion> includes;

  public ServiceConfigurationBase(
    Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments
  ) {
    this(templates, deployments, includes, JsonDocument.newDocument());
  }

  public ServiceConfigurationBase(
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    Collection<ServiceRemoteInclusion> includes,
    JsonDocument properties
  ) {
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }

  public abstract Collection<String> getJvmOptions();

  public abstract Collection<String> getProcessParameters();

  public Collection<ServiceRemoteInclusion> getIncludes() {
    return this.includes;
  }

  public void setIncludes(Collection<ServiceRemoteInclusion> includes) {
    this.includes = includes;
  }

  public Collection<ServiceTemplate> getTemplates() {
    return this.templates;
  }

  public void setTemplates(Collection<ServiceTemplate> templates) {
    this.templates = templates;
  }

  public Collection<ServiceDeployment> getDeployments() {
    return this.deployments;
  }

  public void setDeployments(Collection<ServiceDeployment> deployments) {
    this.deployments = deployments;
  }
}
