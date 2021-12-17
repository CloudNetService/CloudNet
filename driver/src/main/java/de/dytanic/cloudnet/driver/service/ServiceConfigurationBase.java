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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends JsonDocPropertyHolder {

  protected final Collection<ServiceTemplate> templates;
  protected final Collection<ServiceDeployment> deployments;
  protected final Collection<ServiceRemoteInclusion> includes;

  protected ServiceConfigurationBase(
    @NotNull Collection<ServiceTemplate> templates,
    @NotNull Collection<ServiceDeployment> deployments,
    @NotNull Collection<ServiceRemoteInclusion> includes,
    @NotNull JsonDocument properties
  ) {
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }

  public abstract @NotNull Collection<String> jvmOptions();

  public abstract @NotNull Collection<String> processParameters();

  public @NotNull Collection<ServiceRemoteInclusion> includes() {
    return this.includes;
  }

  public @NotNull Collection<ServiceTemplate> templates() {
    return this.templates;
  }

  public @NotNull Collection<ServiceDeployment> deployments() {
    return this.deployments;
  }

  public abstract static class Builder<T extends ServiceConfigurationBase, B extends Builder<T, B>> {

    protected JsonDocument properties = JsonDocument.newDocument();
    protected Collection<String> jvmOptions = new HashSet<>();
    protected Collection<String> processParameters = new HashSet<>();
    protected Collection<ServiceTemplate> templates = new HashSet<>();
    protected Collection<ServiceDeployment> deployments = new HashSet<>();
    protected Collection<ServiceRemoteInclusion> includes = new HashSet<>();

    public @NotNull B properties(@NotNull JsonDocument properties) {
      this.properties = properties;
      return this.self();
    }

    public @NotNull B jvmOptions(@NotNull Collection<String> jvmOptions) {
      this.jvmOptions = new HashSet<>(jvmOptions);
      return this.self();
    }

    public @NotNull B addJvmOption(@NotNull String jvmOption) {
      this.jvmOptions.add(jvmOption);
      return this.self();
    }

    public @NotNull B processParameters(@NotNull Collection<String> processParameters) {
      this.processParameters = new HashSet<>(processParameters);
      return this.self();
    }

    public @NotNull B addProcessParameter(@NotNull String processParameter) {
      this.processParameters.add(processParameter);
      return this.self();
    }

    public @NotNull B templates(@NotNull Collection<ServiceTemplate> templates) {
      this.templates = new HashSet<>(templates);
      return this.self();
    }

    public @NotNull B addTemplate(@NotNull ServiceTemplate template) {
      this.templates.add(template);
      return this.self();
    }

    public @NotNull B deployments(@NotNull Collection<ServiceDeployment> deployments) {
      this.deployments = new HashSet<>(deployments);
      return this.self();
    }

    public @NotNull B addDeployment(@NotNull ServiceDeployment deployment) {
      this.deployments.add(deployment);
      return this.self();
    }

    public @NotNull B includes(@NotNull Collection<ServiceRemoteInclusion> includes) {
      this.includes = new HashSet<>(includes);
      return this.self();
    }

    public @NotNull B addInclude(@NotNull ServiceRemoteInclusion inclusion) {
      this.includes.add(inclusion);
      return this.self();
    }

    protected abstract @NotNull B self();

    public abstract @NotNull T build();
  }
}
