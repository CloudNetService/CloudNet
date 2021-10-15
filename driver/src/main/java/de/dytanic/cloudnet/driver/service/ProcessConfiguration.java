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

import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

@ToString
@EqualsAndHashCode
public class ProcessConfiguration implements Cloneable {

  protected int maxHeapMemorySize;
  protected ServiceEnvironmentType environment;

  protected Set<String> jvmOptions;
  protected Set<String> processParameters;

  protected ProcessConfiguration() {
  }

  public ProcessConfiguration(ServiceEnvironmentType environment, int maxHeapMemorySize) {
    this(environment, maxHeapMemorySize, new HashSet<>());
  }

  public ProcessConfiguration(
    ServiceEnvironmentType environment,
    int maxHeapMemorySize,
    Set<String> jvmOptions
  ) {
    this(maxHeapMemorySize, environment, jvmOptions, new HashSet<>());
  }

  public ProcessConfiguration(
    int maxHeapMemorySize,
    ServiceEnvironmentType environment,
    Set<String> jvmOptions,
    Set<String> processParameters
  ) {
    this.maxHeapMemorySize = maxHeapMemorySize;
    this.environment = environment;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
  }

  public ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  public void setEnvironment(ServiceEnvironmentType environment) {
    this.environment = environment;
  }

  public int getMaxHeapMemorySize() {
    return this.maxHeapMemorySize;
  }

  public void setMaxHeapMemorySize(int maxHeapMemorySize) {
    this.maxHeapMemorySize = maxHeapMemorySize;
  }

  public Set<String> getJvmOptions() {
    return this.jvmOptions;
  }

  public void setJvmOptions(Set<String> jvmOptions) {
    this.jvmOptions = jvmOptions;
  }

  public Set<String> getProcessParameters() {
    return this.processParameters;
  }

  public void setProcessParameters(Set<String> processParameters) {
    this.processParameters = processParameters;
  }

  @Override
  public ProcessConfiguration clone() {
    try {
      return (ProcessConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  /**
   * @deprecated Use {@link #clone()} instead.
   */
  @Deprecated
  @ScheduledForRemoval
  public ProcessConfiguration makeClone() {
    return this.clone();
  }
}
