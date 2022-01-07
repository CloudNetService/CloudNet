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

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GroupConfiguration extends ServiceConfigurationBase implements Cloneable, Nameable {

  protected String name;

  protected Set<String> jvmOptions;
  protected Set<String> processParameters;
  protected Set<String> targetEnvironments;

  protected GroupConfiguration(
    @NonNull String name,
    @NonNull Set<String> jvmOptions,
    @NonNull Set<String> processParameters,
    @NonNull Set<String> targetEnvironments,
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
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
    protected Set<String> targetEnvironments = new HashSet<>();

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
