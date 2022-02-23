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

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * The base class for configuration
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends JsonDocPropertyHolder {

  protected final Set<ServiceTemplate> templates;
  protected final Set<ServiceDeployment> deployments;
  protected final Set<ServiceRemoteInclusion> includes;

  protected ServiceConfigurationBase(
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull JsonDocument properties
  ) {
    super(properties);
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
  }

  public abstract @NonNull Collection<String> jvmOptions();

  public abstract @NonNull Collection<String> processParameters();

  /**
   * Get all includes which should be added initially to services created based on this configuration. These inclusions
   * are downloaded and included either when explicitly requested or before starting the service.
   *
   * @return all inclusions which should get added initially to services created based on this configuration.
   */
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
  public @NonNull Collection<ServiceDeployment> deployments() {
    return this.deployments;
  }

  public abstract static class Builder<T extends ServiceConfigurationBase, B extends Builder<T, B>> {

    protected JsonDocument properties = JsonDocument.newDocument();
    protected Set<String> jvmOptions = new HashSet<>();
    protected Set<String> processParameters = new HashSet<>();
    protected Set<ServiceTemplate> templates = new HashSet<>();
    protected Set<ServiceDeployment> deployments = new HashSet<>();
    protected Set<ServiceRemoteInclusion> includes = new HashSet<>();

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
    public @NonNull B properties(@NonNull JsonDocument properties) {
      this.properties = properties;
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
      this.jvmOptions = new HashSet<>(jvmOptions);
      return this.self();
    }

    /**
     * Adds the given jvm options to this builder. JVM options are there to configure the behaviour of the jvm, for
     * example the garbage collector.
     * <p>
     * The XmX and XmS options will always get appended based on the configured maximum heap memory size.
     * <p>
     * Duplicate options will be omitted by this method directly. <strong>HOWEVER,</strong> adding the same option twice
     * with a changed value to it will most likely result in the jvm to crash, beware!
     *
     * @param jvmOptions the jvm options to add to the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given options collection is null.
     */
    public @NonNull B addJvmOptions(@NonNull Collection<String> jvmOptions) {
      this.jvmOptions.addAll(jvmOptions);
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
      this.processParameters = new HashSet<>(processParameters);
      return this.self();
    }

    /**
     * Adds the process parameters which should get appended to the command line. Process parameters are there to
     * configure the application, for example setting an option like --online-mode=true.
     * <p>
     * This method will override all previously added process parameters options. Furthermore, the given collection will
     * be copied into this builder, meaning that changes to it will not reflect into the builder after the method call.
     * Duplicate parameters will get omitted by this method directly.
     *
     * @param processParameters the process parameters to add to the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given parameters' collection is null.
     */
    public @NonNull B addProcessParameters(@NonNull Collection<String> processParameters) {
      this.processParameters.addAll(processParameters);
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
     * Adds all templates which should get loaded onto a service before it starts. The given collection sorting is
     * ignored and the templates will get re-sorted based on their priority. Templates will override existing files from
     * any source if they are present in them, make sure to use an appropriate order for them.
     * <p>
     * The given collection will be copied into this builder, meaning that changes made to the collection after the
     * method call will not reflect into the builder and vice-versa.
     *
     * @param templates the templates to include onto all services before starting them.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given template collection is null.
     */
    public @NonNull B addTemplates(@NonNull Collection<ServiceTemplate> templates) {
      this.templates.addAll(templates);
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
     * Adds the deployments to execute when a service based on the configuration gets stopped or when explicitly
     * requested by calling the associated method on the service provider.
     * <p>
     * The given collection will be copied into this builder, meaning that changes made to the collection after the
     * method call will not reflect into the builder and vice-versa.
     *
     * @param deployments the deployments to add to every service created based on the configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given deployment collection is null.
     */
    public @NonNull B addDeployments(@NonNull Collection<ServiceDeployment> deployments) {
      this.deployments.addAll(deployments);
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
     * Adds all inclusions which should get loaded onto a service created based on the service configuration before it
     * starts. Inclusions get cached based on their download url. If you need a clean copy of your inclusion you should
     * change the download url of it. If the node is unable to download an inclusion based on the given url it will be
     * ignored and a warning gets printed into the console.
     * <p>
     * The given collection will be copied into this builder, meaning that changes made to the collection after the
     * method call will not reflect into the builder and vice-versa.
     *
     * @param inclusions the inclusions to add include to the previously set inclusions.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given inclusion collection is null.
     */
    public @NonNull B addInclusions(@NonNull Collection<ServiceRemoteInclusion> inclusions) {
      this.includes.addAll(inclusions);
      return this.self();
    }

    protected abstract @NonNull B self();

    public abstract @NonNull T build();
  }
}
