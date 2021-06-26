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

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends SerializableJsonDocPropertyable implements SerializableObject {

  protected Collection<ServiceRemoteInclusion> includes;

  protected Collection<ServiceTemplate> templates;

  protected Collection<ServiceDeployment> deployments;

  public ServiceConfigurationBase(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments) {
    this.includes = includes;
    this.templates = templates;
    this.deployments = deployments;
  }

  public ServiceConfigurationBase() {
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

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeObjectCollection(this.includes);
    buffer.writeObjectCollection(this.templates);
    buffer.writeObjectCollection(this.deployments);
    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.includes = buffer.readObjectCollection(ServiceRemoteInclusion.class);
    this.templates = buffer.readObjectCollection(ServiceTemplate.class);
    this.deployments = buffer.readObjectCollection(ServiceDeployment.class);
    super.read(buffer);
  }
}
