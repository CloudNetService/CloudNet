/*
 * Copyright 2019-2024 CloudNetService team & contributors
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
import com.google.common.collect.ImmutableSet;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a group configuration. A group is used to combine multiple tasks and/or environments together and allows
 * including shared jvm options, process parameters, templates, deployments and includes.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class GroupConfiguration extends ServiceConfigurationBase implements Cloneable, Named {

  protected final String name;

  protected final Set<String> jvmOptions;
  protected final Set<String> processParameters;
  protected final Map<String, String> environmentVariables;

  protected final Set<String> targetEnvironments;

  /**
   * Constructs a new group configuration instance.
   *
   * @param name                 the name of the group.
   * @param jvmOptions           the jvm options of the group to apply to all services inheriting from it.
   * @param processParameters    the process parameters of the group to apply to all services inheriting from it.
   * @param environmentVariables the environment variables to apply to all services inheriting from it.
   * @param targetEnvironments   the environments to apply this group configuration to.
   * @param templates            the templates of the group to apply to all services inheriting from it.
   * @param deployments          the deployments of the group to apply to all services inheriting from it.
   * @param includes             the includes of the group to apply to all services inheriting from it.
   * @param properties           the properties for extra information to store.
   * @throws NullPointerException if one of the given parameters is null.
   */
  protected GroupConfiguration(
    @NonNull String name,
    @NonNull Set<String> jvmOptions,
    @NonNull Set<String> processParameters,
    @NonNull Map<String, String> environmentVariables,
    @NonNull Set<String> targetEnvironments,
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull Document properties
  ) {
    super(templates, deployments, includes, properties);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
    this.environmentVariables = environmentVariables;
  }

  /**
   * Constructs a new builder instance to create a group configuration.
   *
   * @return a new builder for a group.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder which inherits the properties of the given group configuration, allowing to change certain
   * properties of a group. This is useful if you have a group which already exists and just want to change a specific
   * option of it.
   * <p>
   * Calling the build method right after creating the builder will result in a new group which is identical to the
   * given group.
   *
   * @param group the group to copy the properties of.
   * @return a new builder which holds the same properties as the given group.
   * @throws NullPointerException if the given group configuration is null.
   */
  public static @NonNull Builder builder(@NonNull GroupConfiguration group) {
    return builder()
      .name(group.name())
      .jvmOptions(group.jvmOptions())
      .processParameters(group.processParameters())
      .environmentVariables(group.environmentVariables())
      .targetEnvironments(group.targetEnvironments())
      .templates(group.templates())
      .deployments(group.deployments())
      .inclusions(group.inclusions());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Collection<String> jvmOptions() {
    return this.jvmOptions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Collection<String> processParameters() {
    return this.processParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Map<String, String> environmentVariables() {
    return this.environmentVariables;
  }

  /**
   * Get the environments to which this group configuration will always be applied, even if the base task of a service
   * does not implement the group directly. An example use case is a global template which should be applied to all
   * proxies.
   *
   * @return the environments to apply this group configuration always to.
   */
  @Unmodifiable
  public @NonNull Collection<String> targetEnvironments() {
    return this.targetEnvironments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull GroupConfiguration clone() {
    try {
      return (GroupConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen, just explode
    }
  }

  /**
   * Represents a builder for a group configuration.
   *
   * @since 4.0
   */
  public static class Builder extends ServiceConfigurationBase.Builder<GroupConfiguration, Builder> {

    protected String name;
    protected Set<String> targetEnvironments = new HashSet<>();

    /**
     * Sets the name of the newly created group configuration. This property is required in order to build a
     * configuration.
     *
     * @param name the name of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the target environments of the group configuration. This configuration will automatically be applied to all
     * given environments, even if the base task of a service does not implement them.
     * <p>
     * This method overwrites all previously set environments. Furthermore, changes to the given collection after the
     * method call will not get reflected into this builder.
     *
     * @param targetEnvironments the environments to always apply the group configuration to.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment collection is null.
     */
    public @NonNull Builder targetEnvironments(@NonNull Collection<String> targetEnvironments) {
      this.targetEnvironments = new HashSet<>(targetEnvironments);
      return this;
    }

    /**
     * Modifies the target environments of this group configuration. The group configuration will always be applied to
     * all services having the given environment, even if the base task of them does not implement the group.
     *
     * @param modifier the modifier to be applied to the already added target environments of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment is null.
     */
    public @NonNull Builder modifyTargetEnvironments(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.targetEnvironments);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NonNull Builder self() {
      return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if this builder has no name given.
     */
    @Override
    public @NonNull GroupConfiguration build() {
      Preconditions.checkNotNull(this.name, "no name given");
      return new GroupConfiguration(
        this.name,
        ImmutableSet.copyOf(this.jvmOptions),
        ImmutableSet.copyOf(this.processParameters),
        Map.copyOf(this.environmentVariables),
        Set.copyOf(this.targetEnvironments),
        Set.copyOf(this.templates),
        Set.copyOf(this.deployments),
        Set.copyOf(this.includes),
        this.properties.immutableCopy());
    }
  }
}
