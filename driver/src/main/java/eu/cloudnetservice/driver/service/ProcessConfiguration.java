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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Holds the most common options to configure any process started and managed by CloudNet.
 *
 * @param environment       the environment of the task / service this configuration belongs to.
 * @param maxHeapMemorySize the maximum amount of heap (in MB) a service is allowed to use.
 * @param jvmOptions        the jvm options to apply to the process, for example the garbage collector.
 * @param processParameters the process parameters to apply after the actual command line, for example --dev.
 * @since 4.0
 */
public record ProcessConfiguration(
  @NonNull String environment,
  int maxHeapMemorySize,
  @Unmodifiable @NonNull Set<String> jvmOptions,
  @Unmodifiable @NonNull Set<String> processParameters
) implements Cloneable {

  /**
   * Constructs a new builder for a process configuration.
   *
   * @return a new builder for a process configuration.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder for a process configuration copying the properties from the given configuration into the
   * builder. This means that calling build after creating the builder using this method will result in a new process
   * configuration which is equal to the given configuration.
   *
   * @param configuration the configuration to copy the properties of.
   * @return a new builder for a process configuration.
   * @throws NullPointerException if the given process configuration is null.
   */
  public static @NonNull Builder builder(@NonNull ProcessConfiguration configuration) {
    return builder()
      .maxHeapMemorySize(configuration.maxHeapMemorySize())
      .environment(configuration.environment())
      .jvmOptions(configuration.jvmOptions())
      .processParameters(configuration.processParameters());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProcessConfiguration clone() {
    try {
      return (ProcessConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  /**
   * Represents a builder for a process configuration.
   *
   * @since 4.0
   */
  public static class Builder {

    protected String environment;
    protected int maxHeapMemorySize = 512;

    protected Set<String> jvmOptions = new HashSet<>();
    protected Set<String> processParameters = new HashSet<>();

    /**
     * Sets the maximum amount of heap memory (in mb) the service is allowed to use. The given heap memory size must be
     * at least 50 MB (less heap memory makes no sense when running a service).
     *
     * @param maxHeapMemorySize the maximum heap memory a service is allowed to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws IllegalArgumentException if the given memory size is less than 50 mb.
     */
    public @NonNull Builder maxHeapMemorySize(@Range(from = 50, to = Integer.MAX_VALUE) int maxHeapMemorySize) {
      Preconditions.checkArgument(maxHeapMemorySize > 50, "Max heap memory must be at least 50 mb");

      this.maxHeapMemorySize = maxHeapMemorySize;
      return this;
    }

    /**
     * Sets the name of the environment services using the created process configuration should use. Based on the
     * environments the configuration and application files are selected and modified.
     *
     * @param environment the name of the environment the configuration should use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment name is null.
     */
    public @NonNull Builder environment(@NonNull String environment) {
      this.environment = environment;
      return this;
    }

    /**
     * Sets the name of the environment services using the created process configuration should use. Based on the
     * environments the configuration and application files are selected and modified.
     *
     * @param environment the environment the configuration should use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment is null.
     */
    public @NonNull Builder environment(@NonNull ServiceEnvironmentType environment) {
      this.environment = environment.name();
      return this;
    }

    /**
     * Sets the jvm options which should get applied to the service command line. JVM options are there to configure the
     * behaviour of the jvm, for example the garbage collector.
     * <p>
     * The XmX and XmS options will always get appended based on the configured maximum heap memory size.
     * <p>
     * This method will override all previously added jvm options. Furthermore, the given collection will be copied into
     * this builder, meaning that changes to it will not reflect into the builder after the method call.
     *
     * @param jvmOptions the jvm options of the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given options collection is null.
     */
    public @NonNull Builder jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.jvmOptions = new HashSet<>(jvmOptions);
      return this;
    }

    /**
     * Modifies the jvm options of this builder. JVM options are there to configure the behaviour of the jvm, for
     * example the garbage collector.
     * <p>
     * The XmX and XmS options will always get appended based on the configured maximum heap memory size.
     * <p>
     * Duplicate options will be omitted by the underlying collection. <strong>HOWEVER,</strong> adding the same option
     * twice with a changed value to it will most likely result in the jvm to crash, beware!
     *
     * @param modifier the modifier to be applied to the already added jvm options of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given options collection is null.
     */
    public @NonNull Builder modifyJvmOptions(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.jvmOptions);
      return this;
    }

    /**
     * Sets the process parameters which should get appended to the command line. Process parameters are there to
     * configure the application, for example setting an option like --online-mode=true.
     * <p>
     * This method will override all previously added process parameters options. Furthermore, the given collection will
     * be copied into this builder, meaning that changes to it will not reflect into the builder after the method call.
     *
     * @param processParameters the process parameters of the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given parameters' collection is null.
     */
    public @NonNull Builder processParameters(@NonNull Collection<String> processParameters) {
      this.processParameters = new HashSet<>(processParameters);
      return this;
    }

    /**
     * Modifies the process parameters which should get appended to the command line. Process parameters are there to
     * configure the application, for example setting an option like --online-mode=true.
     * <p>
     * Duplicate parameters will get omitted by the underlying collection.
     *
     * @param modifier the modifier to be applied to the already added process parameters of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given parameters' collection is null.
     */
    public @NonNull Builder modifyProcessParameters(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.processParameters);
      return this;
    }

    /**
     * Builds the process configuration based on the configuration of this builder.
     *
     * @return the created process configuration.
     * @throws NullPointerException if no environment is specified for the configuration.
     */
    public @NonNull ProcessConfiguration build() {
      Preconditions.checkNotNull(this.environment, "no environment given");

      return new ProcessConfiguration(
        this.environment,
        this.maxHeapMemorySize,
        Set.copyOf(this.jvmOptions),
        Set.copyOf(this.processParameters));
    }
  }
}
