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
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A service template holds all files which can be copied onto a service. They are building the base structure of any
 * dynamic service and can be included on them dynamically as well.
 *
 * @since 4.0
 */
@EqualsAndHashCode
public class ServiceTemplate implements Named, Comparable<ServiceTemplate>, Cloneable {

  /**
   * The name of the local template storage.
   */
  public static final String LOCAL_STORAGE = "local";

  private final String prefix;
  private final String name;
  private final String storage;

  private final int priority;
  private final boolean alwaysCopyToStaticServices;

  /**
   * Constructs a new service template instance.
   *
   * @param prefix                     the prefix of the template.
   * @param name                       the actual name of the template.
   * @param storage                    the storage in which the template is stored.
   * @param priority                   the inclusion priority of the template.
   * @param alwaysCopyToStaticServices if the template should always get copied onto static services.
   * @throws NullPointerException if one of the given parameters is null.
   */
  protected ServiceTemplate(
    @NonNull String prefix,
    @NonNull String name,
    @NonNull String storage,
    int priority,
    boolean alwaysCopyToStaticServices
  ) {
    this.prefix = prefix;
    this.name = name;
    this.storage = storage;
    this.priority = priority;
    this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
  }

  /**
   * Constructs a new builder instance for a service template.
   *
   * @return a new service template builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder for a service template which has all properties of the given template already set.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in a service template
   * which is equal but not the same as the given one.
   *
   * @param template the template to copy the properties of.
   * @return a builder for a service template which has the properties of the given template already set.
   * @throws NullPointerException if the given service template is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceTemplate template) {
    return builder()
      .name(template.name())
      .prefix(template.prefix())
      .storage(template.storageName())
      .priority(template.priority())
      .alwaysCopyToStaticServices(template.alwaysCopyToStaticServices());
  }

  /**
   * Parses a service template from the given input string if possible. The input template must be in the form
   * <ol>
   *   <li>storage:prefix/name
   *   <li>prefix/name (in this case the storage of the template will be the local storage)
   * </ol>
   * <p>
   * Note: the returned service template only contains data actually given to the method, therefore the priority of the
   * template will always be 0 and alwaysCopyToStaticServices false.
   *
   * @param template the input string to parse if possible.
   * @return the parsed service template from the given input, null if parsing was not possible.
   * @throws NullPointerException if the given input string is null.
   */
  public static @Nullable ServiceTemplate parse(@NonNull String template) {
    // check if the template contains a storage-name splitter
    var parts = template.split(":");
    if (parts.length == 0 || parts.length > 2) {
      return null;
    }
    // read the storage and name path
    var path = parts.length == 2 ? parts[1] : parts[0];
    var storage = parts.length == 2 ? parts[0] : LOCAL_STORAGE;
    // validate the name path
    var splitPath = path.split("/");
    if (splitPath.length != 2) {
      return null;
    }
    // creates the new template
    return builder()
      .prefix(splitPath[0])
      .name(splitPath[1])
      .storage(storage)
      .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the prefix of the template. The prefix is used to group templates together, for example for a service task.
   *
   * @return the prefix of the template.
   */
  public @NonNull String prefix() {
    return this.prefix;
  }

  /**
   * Get the name of the storage in which the template is located.
   *
   * @return the storage of the template.
   */
  public @NonNull String storageName() {
    return this.storage;
  }

  /**
   * This priority is used to determine in which order the template should be loaded onto a service. For example a
   * template with a priority of 10 (high priority) is installed after templates with a lower priority (for example 1).
   * This can for example be used to control the order of file copies, for example when they potentially override each
   * other.
   *
   * @return the priority of the template.
   */
  public int priority() {
    return this.priority;
  }

  /**
   * Get if this template should always be copied onto static services. By default, a template is only copied onto a
   * static service when it starts for the first time. Note that if enabled file from this template can override files
   * which were created by the static service.
   *
   * @return true if this template should always be copied onto static services, false otherwise.
   */
  public boolean alwaysCopyToStaticServices() {
    return this.alwaysCopyToStaticServices;
  }

  /**
   * Get the full name of the template without the storage prefix. If you need a name with storage, use toString
   * instead.
   *
   * @return the full name of the template in the format: prefix/name.
   */
  public @NonNull String fullName() {
    return this.prefix + '/' + this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.storage + ':' + this.prefix + '/' + this.name;
  }

  /**
   * Get the template storage in which this template is stored, throwing an exception if the template doesn't exist.
   *
   * @return the template storage in which this template is stored.
   * @throws NullPointerException if the storage used in this template is unknown.
   */
  public @NonNull TemplateStorage storage() {
    var storage = this.findStorage();
    Preconditions.checkNotNull(storage, "the storage of this template does not exist");

    return storage;
  }

  /**
   * Tries to find the storage in which this template is stored. This method returns null rather than throwing an
   * exception if the storage doesn't exist.
   *
   * @return the template storage in which this template is stored, null if the template doesn't exist.
   */
  public @Nullable TemplateStorage findStorage() {
    var storageProvider = InjectionLayer.boot().instance(TemplateStorageProvider.class);
    return storageProvider.templateStorage(this.storage);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Range(from = -1, to = 1) int compareTo(@NonNull ServiceTemplate serviceTemplate) {
    return Integer.compare(this.priority, serviceTemplate.priority);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceTemplate clone() {
    try {
      return (ServiceTemplate) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * A builder for a template storage.
   *
   * @since 4.0
   */
  public static class Builder {

    private String name;
    private String prefix;
    private String storage = ServiceTemplate.LOCAL_STORAGE;

    private int priority;
    private boolean alwaysCopyToStaticServices;

    /**
     * Sets the name of the template. The name is the main identification point of the template and can be for example
     * 2x2 if you want to name a map for 4 players.
     *
     * @param name the name of the template.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the prefix of the template. This can be used to group templates together, for example all templates for
     * BedWars. The names of the templates would be the map sized (for example 2x2 or 3x3), grouped by the prefix of the
     * template, BedWars.
     *
     * @param prefix the prefix of the template.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given prefix is null.
     */
    public @NonNull Builder prefix(@NonNull String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Sets the storage of the template. No check is made if the storage given to this method actually exists.
     *
     * @param storage the storage of the template.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given storage name is null.
     */
    public @NonNull Builder storage(@NonNull String storage) {
      this.storage = storage;
      return this;
    }

    /**
     * Sets the priority of the template. The priority is used to control the order in which templates get loaded onto a
     * service to prevent for example accidental overrides of files which are located in multiple templates. The
     * template with the highest priority will be loaded at the end.
     *
     * @param priority the priority of the template.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    /**
     * Sets the if the template should always get copied onto static services. By default, all templates will only get
     * copied once at the initial startup of the static service.
     *
     * @param alwaysCopyToStaticServices if the template should always be copied onto static services.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder alwaysCopyToStaticServices(boolean alwaysCopyToStaticServices) {
      this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
      return this;
    }

    /**
     * Builds the service template based on this builder.
     *
     * @return the created service template.
     * @throws NullPointerException if no name or prefix was given.
     */
    public @NonNull ServiceTemplate build() {
      Preconditions.checkNotNull(this.name, "no name given");
      Preconditions.checkNotNull(this.prefix, "no prefix given");

      return new ServiceTemplate(this.prefix, this.name, this.storage, this.priority, this.alwaysCopyToStaticServices);
    }
  }
}
