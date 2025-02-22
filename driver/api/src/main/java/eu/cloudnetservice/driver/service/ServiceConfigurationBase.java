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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The base configuration class for everything which can configure a service in any way. It holds the most common
 * information which every component managing a service should provide to a user.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public abstract class ServiceConfigurationBase implements DefaultedDocPropertyHolder {

  protected final Set<ServiceTemplate> templates;
  protected final Set<ServiceDeployment> deployments;
  protected final Set<ServiceRemoteInclusion> includes;

  protected final Document properties;

  /**
   * Constructs a new service configuration base instance.
   *
   * @param templates   the templates which should be loaded to all services created based on the configuration.
   * @param deployments the deployment which should be added to all services created based on the configuration.
   * @param includes    the includes which should be loaded to all services created based on the configuration.
   * @param properties  the properties of the configuration.
   * @throws NullPointerException if one of the constructor parameters are null.
   */
  protected ServiceConfigurationBase(
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull Document properties
  ) {
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }

  /**
   * Get the jvm options which should get applied to the service command line. JVM options are there to configure the
   * behaviour of the jvm, for example the garbage collector.
   *
   * @return the jvm options to set for services created based on this configuration.
   */
  @Unmodifiable
  public abstract @NonNull Collection<String> jvmOptions();

  /**
   * Get the process parameters which should get appended to the command line. Process parameters are there to configure
   * the application, for example setting an option like --online-mode=true.
   *
   * @return the process parameters to set for services created based on this configuration.
   */
  @Unmodifiable
  public abstract @NonNull Collection<String> processParameters();

  /**
   * Get the environment variables which should get appended to all environments of services which are created based on
   * this configuration.
   *
   * @return the environment variables to set for services created based on this configuration.
   */
  @Unmodifiable
  public abstract @NonNull Map<String, String> environmentVariables();

  /**
   * Get all includes which should be added initially to services created based on this configuration. These inclusions
   * are downloaded and included either when explicitly requested or before starting the service.
   *
   * @return all inclusions which should get added initially to services created based on this configuration.
   */
  @Unmodifiable
  public @NonNull Collection<ServiceRemoteInclusion> inclusions() {
    return this.includes;
  }

  /**
   * Get all templates that should get copied onto services created based on this configuration when preparing.
   * Templates of configured groups and groups targeting the environment of this configuration will be included
   * automatically.
   * <p>
   * <strong>NOTE:</strong> the returned set is not yet sorted, sorting will be made when actually including the
   * templates. The sorting changes will not reflect into this configuration.
   *
   * @return all templates to include when preparing a service based on this configuration.
   */
  @Unmodifiable
  public @NonNull Collection<ServiceTemplate> templates() {
    return this.templates;
  }

  /**
   * Get all deployments that should be added initially to services created based on this configuration. These
   * deployments will not get executed directly, you need to execute them for each service individually using the
   * service provider.
   *
   * @return all deployments which should get added initially when preparing a service.
   */
  @Unmodifiable
  public @NonNull Collection<ServiceDeployment> deployments() {
    return this.deployments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }

  /**
   * The base builder class for everything which is a service configuration.
   *
   * @param <T> the type which gets build by the builder.
   * @param <B> the builder instance from the type which gets build, for proper chaining purposes.
   * @since 4.0
   */
  public abstract static class Builder<T extends ServiceConfigurationBase, B extends Builder<T, B>>
    implements Mutable.WithDirectModifier<B> {

    protected Set<String> jvmOptions = new LinkedHashSet<>();
    protected Set<String> processParameters = new LinkedHashSet<>();

    protected Set<ServiceTemplate> templates = new HashSet<>();
    protected Set<ServiceDeployment> deployments = new HashSet<>();
    protected Set<ServiceRemoteInclusion> includes = new HashSet<>();

    protected Map<String, String> environmentVariables = new HashMap<>();

    protected Document.Mutable properties = Document.newJsonDocument();

    /**
     * The properties to apply to the underlying created configuration.
     * <p>
     * Note: some plugins might override certain properties, for example the bridge plugin overrides the online count
     * property if set. There is no way to prevent this, as it ensures that CloudNet can run correctly. Just make sure
     * that your property names differ from them CloudNet uses by default.
     *
     * @param properties the properties to apply by default to all services.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties document is null.
     */
    public @NonNull B properties(@NonNull Document properties) {
      this.properties = properties.mutableCopy();
      return this.self();
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
    public @NonNull B jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.jvmOptions = new LinkedHashSet<>(jvmOptions);
      return this.self();
    }

    /**
     * Modifies the jvm options of this builder. JVM options are there to configure the behaviour of the jvm, for
     * example the garbage collector.
     * <p>
     * The XmX and XmS options will always get appended based on the configured maximum heap memory size.
     * <p>
     * Duplicate options will be omitted by this method directly. <strong>HOWEVER,</strong> adding the same option twice
     * with a changed value to it will most likely result in the jvm to crash, beware!
     *
     * @param modifier the modifier to be applied to the already added jvm options of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given options collection is null.
     */
    public @NonNull B modifyJvmOptions(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.jvmOptions);
      return this.self();
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
    public @NonNull B processParameters(@NonNull Collection<String> processParameters) {
      this.processParameters = new LinkedHashSet<>(processParameters);
      return this.self();
    }

    /**
     * Modifies the process parameters which should get appended to the command line. Process parameters are there to
     * configure the application, for example setting an option like --online-mode=true.
     * <p>
     * Duplicate parameters will get omitted by this method directly.
     *
     * @param modifier the modifier to be applied to the already added process parameters of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given parameters' collection is null.
     */
    public @NonNull B modifyProcessParameters(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.processParameters);
      return this.self();
    }

    /**
     * Sets all templates which should get loaded onto a service before it starts. The given collection sorting is
     * ignored and the templates will get re-sorted based on their priority. Templates will override existing files from
     * any source if they are present in them, make sure to use an appropriate order for them.
     * <p>
     * This method will override all previously added templates. The given collection will be copied into this builder,
     * meaning that changes made to the collection after the method call will not reflect into the builder and
     * vice-versa.
     *
     * @param templates the templates to include onto all services before starting them.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given template collection is null.
     */
    public @NonNull B templates(@NonNull Collection<ServiceTemplate> templates) {
      this.templates = new HashSet<>(templates);
      return this.self();
    }

    /**
     * Modifies the templates which should get loaded onto a service before it starts. The given collection sorting is
     * ignored and the templates will get re-sorted based on their priority. Templates will override existing files from
     * any source if they are present in them, make sure to use an appropriate order for them.
     *
     * @param modifier the modifier to be applied to the already added templates of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given template collection is null.
     */
    public @NonNull B modifyTemplates(@NonNull Consumer<Collection<ServiceTemplate>> modifier) {
      modifier.accept(this.templates);
      return this.self();
    }

    /**
     * Sets the deployments to execute when a service based on the configuration gets stopped or when explicitly
     * requested by calling the associated method on the service provider.
     * <p>
     * This method will override all previously added deployments. The given collection will be copied into this
     * builder, meaning that changes made to the collection after the method call will not reflect into the builder and
     * vice-versa.
     *
     * @param deployments the deployments to add to every service created based on the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given deployment collection is null.
     */
    public @NonNull B deployments(@NonNull Collection<ServiceDeployment> deployments) {
      this.deployments = new HashSet<>(deployments);
      return this.self();
    }

    /**
     * Modifies the deployments to execute when a service based on the configuration gets stopped or when explicitly
     * requested by calling the associated method on the service provider.
     *
     * @param modifier the modifier to be applied to the already added deployments of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given deployment collection is null.
     */
    public @NonNull B modifyDeployments(@NonNull Consumer<Collection<ServiceDeployment>> modifier) {
      modifier.accept(this.deployments);
      return this.self();
    }

    /**
     * Sets all inclusions which should get loaded onto a service created based on the service configuration before it
     * starts. Inclusions get cached based on their download url. If you need a clean copy of your inclusion you should
     * change the download url of it. If the node is unable to download an inclusion based on the given url it will be
     * ignored and a warning gets printed into the console.
     * <p>
     * This method overrides all previously added inclusions. The given collection will be copied into this builder,
     * meaning that changes made to the collection after the method call will not reflect into the builder and
     * vice-versa.
     *
     * @param inclusions the inclusions to include on all services created based on the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given inclusion collection is null.
     */
    public @NonNull B inclusions(@NonNull Collection<ServiceRemoteInclusion> inclusions) {
      this.includes = new HashSet<>(inclusions);
      return this.self();
    }

    /**
     * Modifies the inclusions which should get loaded onto a service created based on the service configuration before
     * it starts. Inclusions get cached based on their download url. If you need a clean copy of your inclusion you
     * should change the download url of it. If the node is unable to download an inclusion based on the given url it
     * will be ignored and a warning gets printed into the console.
     *
     * @param modifier the modifier to be applied to the already added inclusions of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given inclusion collection is null.
     */
    public @NonNull B modifyInclusions(@NonNull Consumer<Collection<ServiceRemoteInclusion>> modifier) {
      modifier.accept(this.includes);
      return this.self();
    }


    /**
     * Sets the environment variables which should set in the environment the process runs in.
     * <p>
     * This method will override all previously added environment variables options. Furthermore, the given map will be
     * copied into this builder, meaning that changes to it will not reflect into the builder after the method call.
     *
     * @param environmentVariables the environment variables to apply to processes created based on this configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment variables map is null.
     */
    public @NonNull B environmentVariables(@NonNull Map<String, String> environmentVariables) {
      this.environmentVariables = new HashMap<>(environmentVariables);
      return this.self();
    }

    /**
     * Modifies the environment variables which should get appended to the environment of the process.
     *
     * @param modifier the modifier to be applied to the already added environment variables of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the modifier is null.
     */
    public @NonNull B modifyEnvironmentVariables(@NonNull Consumer<Map<String, String>> modifier) {
      modifier.accept(this.environmentVariables);
      return this.self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document.@NonNull Mutable propertyHolder() {
      return this.properties;
    }

    /**
     * Get the current instance of the builder, this removes the need for unchecked generics which are annoying.
     *
     * @return the current instance of this builder.
     */
    protected abstract @NonNull B self();

    /**
     * Builds an instance of the service configuration type this builder is targeting.
     *
     * @return the build service configuration.
     */
    public abstract @NonNull T build();
  }
}
