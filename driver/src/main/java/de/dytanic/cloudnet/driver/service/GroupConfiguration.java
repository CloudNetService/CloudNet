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

package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GroupConfiguration extends ServiceConfigurationBase implements Cloneable, Nameable {

  protected String name;

  protected Collection<String> jvmOptions;
  protected Collection<String> processParameters;
  protected Collection<String> targetEnvironments;

  protected GroupConfiguration(
    @NonNull String name,
    @NonNull Collection<String> jvmOptions,
    @NonNull Collection<String> processParameters,
    @NonNull Collection<String> targetEnvironments,
    @NonNull Collection<ServiceTemplate> templates,
    @NonNull Collection<ServiceDeployment> deployments,
    @NonNull Collection<ServiceRemoteInclusion> includes,
    @NonNull JsonDocument properties
  ) {
    super(templates, deployments, includes, properties);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NonNull Collection<String> jvmOptions() {
    return this.jvmOptions;
  }

  @Override
  public @NonNull Collection<String> processParameters() {
    return this.processParameters;
  }

  public @NonNull Collection<String> targetEnvironments() {
    return this.targetEnvironments;
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public @NonNull GroupConfiguration clone() {
    try {
      return (GroupConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  public static class Builder extends ServiceConfigurationBase.Builder<GroupConfiguration, Builder> {

    protected String name;
    protected Collection<String> targetEnvironments = new HashSet<>();

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder targetEnvironments(@NonNull Collection<String> targetEnvironments) {
      this.targetEnvironments = new HashSet<>(targetEnvironments);
      return this;
    }

    public @NonNull Builder addTargetEnvironment(@NonNull String environmentType) {
      this.targetEnvironments.add(environmentType);
      return this;
    }

    @Override
    protected @NonNull Builder self() {
      return this;
    }

    @Override
    public @NonNull GroupConfiguration build() {
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
