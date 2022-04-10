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
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * A deployment for a service. A deployment can be added to any service and will copy the data from the currently
 * running service into the given template, allowing for example cross copy of templates as there is no need for a
 * service to be based on the given template to get copied to it.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceDeployment extends JsonDocPropertyHolder implements Cloneable {

  protected final ServiceTemplate template;
  protected final Collection<String> excludes;

  /**
   * Constructs a new service deployment instance.
   *
   * @param template   the template to deploy the service data to.
   * @param excludes   the names of the files to exclude.
   * @param properties the properties for the deployment, not used internally and mainly for identification reasons.
   * @throws NullPointerException if one of the constructor parameters is null.
   */
  protected ServiceDeployment(
    @NonNull ServiceTemplate template,
    @NonNull Collection<String> excludes,
    @NonNull JsonDocument properties
  ) {
    super(properties);
    this.template = template;
    this.excludes = excludes;
  }

  /**
   * Creates a new builder for a service deployment.
   *
   * @return a new service deployment builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new builder for a service deployment which has the same properties pre-set as the given deployment
   * instance. Changes made to the given deployment will not reflect into the builder and vice-versa.
   * <p>
   * When calling build directly after creating the builder with this method it will return a service deployment
   * instance which is equal to the given one but not identical.
   *
   * @param deployment the deployment to copy the properties of.
   * @return a new service builder.
   * @throws NullPointerException if the given deployment is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceDeployment deployment) {
    return builder()
      .template(deployment.template())
      .excludes(deployment.excludes())
      .properties(deployment.properties().clone());
  }

  /**
   * Get the template to which this deployment (and therefore the service files) should get copied.
   *
   * @return the target template of this deployment.
   */
  public @NonNull ServiceTemplate template() {
    return this.template;
  }

  /**
   * Get a collection of file names (in a regular expression format) which should get excluded from a deployment.
   * Directories must be suffixed with a {@literal /}.
   *
   * @return the regular expressions of file names which should get excluded from a deployment.
   */
  public @NonNull Collection<String> excludes() {
    return this.excludes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceDeployment clone() {
    try {
      return (ServiceDeployment) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * A builder for a service deployment.
   *
   * @since 4.0
   */
  public static class Builder {

    protected ServiceTemplate template;
    protected Collection<String> excludes = new HashSet<>();
    protected JsonDocument properties = JsonDocument.newDocument();

    /**
     * Sets the target template of the deployment. There is no need for the template to exist, nor for the template to
     * be added to the target service of this deployment.
     *
     * @param template the target template of the deployment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given template is null.
     */
    public @NonNull Builder template(@NonNull ServiceTemplate template) {
      this.template = template;
      return this;
    }

    /**
     * Sets the file names (in a regular expression form) which should be ignored when deploying a service. Directories
     * must be suffixed with a {@literal /}.
     * <p>
     * The given exclusion collection will get copied into the builder, meaning that further modification of it will not
     * reflect into the builder and vice-versa.
     *
     * @param excludes the excludes of the deployment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if then given exclusion collection is null.
     */
    public @NonNull Builder excludes(@NonNull Collection<String> excludes) {
      this.excludes = new HashSet<>(excludes);
      return this;
    }

    /**
     * Adds the file names (in a regular expression form) which should be ignored when deploying a service. Directories
     * must be suffixed with a {@literal /}.
     * <p>
     * The given exclusion collection will get copied into the builder, meaning that further modification of it will not
     * reflect into the builder and vice-versa.
     *
     * @param excludes the excludes to add to the deployment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if then given exclusion collection is null.
     */
    public @NonNull Builder addExcludes(@NonNull Collection<String> excludes) {
      this.excludes.addAll(excludes);
      return this;
    }

    /**
     * Sets the properties of the deployment. These properties will not be used internally and are mainly there for
     * different implementation and/or identification by modules or plugins if needed.
     *
     * @param properties the properties of this deployment.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties document is null.
     */
    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties.clone();
      return this;
    }

    /**
     * Builds a new service deployment instances based on this builder.
     *
     * @return a service deployment from this builder.
     * @throws NullPointerException if no template was set in this builder.
     */
    public @NonNull ServiceDeployment build() {
      Preconditions.checkNotNull(this.template, "no target template given");
      return new ServiceDeployment(this.template, this.excludes, this.properties);
    }
  }
}
