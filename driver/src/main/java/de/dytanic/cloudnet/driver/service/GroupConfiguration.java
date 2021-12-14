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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GroupConfiguration extends ServiceConfigurationBase implements Cloneable, INameable {

  protected String name;

  protected Collection<String> jvmOptions;
  protected Collection<String> processParameters;
  protected Collection<String> targetEnvironments;

  protected GroupConfiguration(
    @NotNull String name,
    @NotNull Collection<String> jvmOptions,
    @NotNull Collection<String> processParameters,
    @NotNull Collection<String> targetEnvironments,
    @NotNull Collection<ServiceTemplate> templates,
    @NotNull Collection<ServiceDeployment> deployments,
    @NotNull Collection<ServiceRemoteInclusion> includes,
    @NotNull JsonDocument properties
  ) {
    super(templates, deployments, includes, properties);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NotNull Collection<String> getJvmOptions() {
    return this.jvmOptions;
  }

  @Override
  public @NotNull Collection<String> getProcessParameters() {
    return this.processParameters;
  }

  public @NotNull Collection<String> getTargetEnvironments() {
    return this.targetEnvironments;
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  @Override
  public @NotNull GroupConfiguration clone() {
    try {
      return (GroupConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  public static class Builder extends ServiceConfigurationBase.Builder<GroupConfiguration, Builder> {

    protected String name;
    protected Collection<String> targetEnvironments = new HashSet<>();

    public @NotNull Builder name(@NotNull String name) {
      this.name = name;
      return this;
    }

    public @NotNull Builder targetEnvironments(@NotNull Collection<String> targetEnvironments) {
      this.targetEnvironments = new HashSet<>(targetEnvironments);
      return this;
    }

    public @NotNull Builder addTargetEnvironment(@NotNull String environmentType) {
      this.targetEnvironments.add(environmentType);
      return this;
    }

    @Override
    protected @NotNull Builder self() {
      return this;
    }

    @Override
    public @NotNull GroupConfiguration build() {
      Verify.verifyNotNull(this.name, "no name given");
      return new GroupConfiguration(
        this.name,
        this.jvmOptions,
        this.processParameters,
        this.targetEnvironments,
        this.templates,
        this.deployments,
        this.includes,
        this.properties);
    }
  }
}
