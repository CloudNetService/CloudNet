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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public record ProcessConfiguration(
  @NotNull String environment,
  int maxHeapMemorySize,
  @NotNull Set<String> jvmOptions,
  @NotNull Set<String> processParameters
) implements Cloneable {

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ProcessConfiguration configuration) {
    return builder()
      .maxHeapMemorySize(configuration.maxHeapMemorySize())
      .environment(configuration.environment())
      .jvmOptions(configuration.jvmOptions())
      .processParameters(configuration.processParameters());
  }

  @Override
  public ProcessConfiguration clone() {
    try {
      return (ProcessConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  public static class Builder {

    protected String environment;
    protected int maxHeapMemorySize = 512;

    protected Set<String> jvmOptions = new HashSet<>();
    protected Set<String> processParameters = new HashSet<>();

    public @NotNull Builder maxHeapMemorySize(@Range(from = 50, to = Integer.MAX_VALUE) int maxHeapMemorySize) {
      this.maxHeapMemorySize = maxHeapMemorySize;
      return this;
    }

    public @NotNull Builder environment(@NotNull String environment) {
      this.environment = environment;
      return this;
    }

    public @NotNull Builder environment(@NotNull ServiceEnvironmentType environment) {
      this.environment = environment.name();
      return this;
    }

    public @NotNull Builder jvmOptions(@NotNull Collection<String> jvmOptions) {
      this.jvmOptions = new HashSet<>(jvmOptions);
      return this;
    }

    public @NotNull Builder addJvmOption(@NotNull String jvmOption) {
      this.jvmOptions.add(jvmOption);
      return this;
    }

    public @NotNull Builder processParameters(@NotNull Collection<String> processParameters) {
      this.processParameters = new HashSet<>(processParameters);
      return this;
    }

    public @NotNull Builder addProcessParameter(@NotNull String processParameter) {
      this.processParameters.add(processParameter);
      return this;
    }

    public @NotNull ProcessConfiguration build() {
      Verify.verifyNotNull(this.environment, "no environment given");
      Verify.verify(this.maxHeapMemorySize >= 50, "heap memory must be at least 50 MB");

      return new ProcessConfiguration(
        this.environment,
        this.maxHeapMemorySize,
        this.jvmOptions,
        this.processParameters);
    }
  }
}
