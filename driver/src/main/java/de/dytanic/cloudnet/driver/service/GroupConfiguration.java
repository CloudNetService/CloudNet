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

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GroupConfiguration extends ServiceConfigurationBase implements INameable {

  protected String name;

  protected Collection<String> jvmOptions;
  protected Collection<String> processParameters;
  protected Collection<ServiceEnvironmentType> targetEnvironments;

  public GroupConfiguration(
    Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name,
    Collection<String> jvmOptions,
    Collection<ServiceEnvironmentType> targetEnvironments
  ) {
    this(includes, templates, deployments, name, jvmOptions, new ArrayList<>(), targetEnvironments);
  }

  public GroupConfiguration(
    Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name,
    Collection<String> jvmOptions,
    Collection<String> processParameters,
    Collection<ServiceEnvironmentType> targetEnvironments
  ) {
    super(includes, templates, deployments);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
  }

  public GroupConfiguration(
    String name,
    Collection<String> jvmOptions,
    Collection<String> processParameters,
    Collection<ServiceEnvironmentType> targetEnvironments,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    Collection<ServiceRemoteInclusion> includes,
    JsonDocument properties
  ) {
    super(templates, deployments, includes, properties);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
  }

  @Contract("_, _ -> new")
  public static @NotNull GroupConfiguration empty(@NotNull String name, @Nullable ServiceEnvironmentType type) {
    return new GroupConfiguration(
      new ArrayList<>(),
      new ArrayList<>(),
      new ArrayList<>(),
      name,
      new ArrayList<>(),
      new ArrayList<>(type == null ? Collections.emptySet() : Collections.singleton(type)));
  }

  @Override
  public Collection<String> getJvmOptions() {
    return this.jvmOptions;
  }

  @Override
  public Collection<String> getProcessParameters() {
    return this.processParameters;
  }

  public Collection<ServiceEnvironmentType> getTargetEnvironments() {
    return this.targetEnvironments;
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }
}
