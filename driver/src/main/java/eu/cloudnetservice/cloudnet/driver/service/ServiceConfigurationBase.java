/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.service;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends JsonDocPropertyHolder {

  protected final Set<ServiceTemplate> templates;
  protected final Set<ServiceDeployment> deployments;
  protected final Set<ServiceRemoteInclusion> includes;

  protected ServiceConfigurationBase(
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull JsonDocument properties
  ) {
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }

  public abstract @NonNull Collection<String> jvmOptions();

  public abstract @NonNull Collection<String> processParameters();

  public @NonNull Collection<ServiceRemoteInclusion> includes() {
    return this.includes;
  }

  public @NonNull Collection<ServiceTemplate> templates() {
    return this.templates;
  }

  public @NonNull Collection<ServiceDeployment> deployments() {
    return this.deployments;
  }

  public abstract static class Builder<T extends ServiceConfigurationBase, B extends Builder<T, B>> {

    protected JsonDocument properties = JsonDocument.newDocument();
    protected Set<String> jvmOptions = new HashSet<>();
    protected Set<String> processParameters = new HashSet<>();
    protected Set<ServiceTemplate> templates = new HashSet<>();
    protected Set<ServiceDeployment> deployments = new HashSet<>();
    protected Set<ServiceRemoteInclusion> includes = new HashSet<>();

    public @NonNull B properties(@NonNull JsonDocument properties) {
      this.properties = properties;
      return this.self();
    }

    public @NonNull B jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.jvmOptions = new HashSet<>(jvmOptions);
      return this.self();
    }

    public @NonNull B addJvmOption(@NonNull String jvmOption) {
      this.jvmOptions.add(jvmOption);
      return this.self();
    }

    public @NonNull B processParameters(@NonNull Collection<String> processParameters) {
      this.processParameters = new HashSet<>(processParameters);
      return this.self();
    }

    public @NonNull B addProcessParameter(@NonNull String processParameter) {
      this.processParameters.add(processParameter);
      return this.self();
    }

    public @NonNull B templates(@NonNull Collection<ServiceTemplate> templates) {
      this.templates = new HashSet<>(templates);
      return this.self();
    }

    public @NonNull B addTemplate(@NonNull ServiceTemplate template) {
      this.templates.add(template);
      return this.self();
    }

    public @NonNull B deployments(@NonNull Collection<ServiceDeployment> deployments) {
      this.deployments = new HashSet<>(deployments);
      return this.self();
    }

    public @NonNull B addDeployment(@NonNull ServiceDeployment deployment) {
      this.deployments.add(deployment);
      return this.self();
    }

    public @NonNull B includes(@NonNull Collection<ServiceRemoteInclusion> includes) {
      this.includes = new HashSet<>(includes);
      return this.self();
    }

    public @NonNull B addInclude(@NonNull ServiceRemoteInclusion inclusion) {
      this.includes.add(inclusion);
      return this.self();
    }

    protected abstract @NonNull B self();

    public abstract @NonNull T build();
  }
}
