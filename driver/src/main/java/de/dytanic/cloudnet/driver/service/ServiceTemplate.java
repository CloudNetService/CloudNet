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
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.util.Arrays;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Defines the location of a template for services that can either be copied into a service or filled from a service by
 * using a {@link ServiceDeployment}. CloudNet's default storage is "local".
 */
@EqualsAndHashCode
public class ServiceTemplate implements INameable, Comparable<ServiceTemplate>, Cloneable {

  public static final String LOCAL_STORAGE = "local";

  private final String prefix;
  private final String name;
  private final String storage;

  private final int priority;
  private final boolean alwaysCopyToStaticServices;

  protected ServiceTemplate(
    @NotNull String prefix,
    @NotNull String name,
    @NotNull String storage,
    int priority,
    boolean alwaysCopyToStaticServices
  ) {
    this.prefix = prefix;
    this.name = name;
    this.storage = storage;
    this.priority = priority;
    this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ServiceTemplate template) {
    return builder()
      .name(template.getName())
      .prefix(template.getPrefix())
      .storage(template.getStorage())
      .priority(template.getPriority())
      .alwaysCopyToStaticServices(template.shouldAlwaysCopyToStaticServices());
  }

  /**
   * Parses a template out of a string in the following format: storage:prefix/name "storage:" is optional, only
   * "prefix/name" needs to be provided
   * <p>
   * {@code alwaysCopyToStaticServices} will always be false in the returned {@link ServiceTemplate}.
   *
   * @param template the template in the specified format
   * @return the parsed {@link ServiceTemplate} or null if the format was invalid
   */
  public static @Nullable ServiceTemplate parse(@NotNull String template) {
    // check if the template contains a storage-name splitter
    var parts = template.split(":");
    if (parts.length == 0 || parts.length > 2) {
      return null;
    }
    // read the storage and name path
    var path = parts.length == 2 ? parts[1] : parts[0];
    var storage = parts.length == 2 ? parts[0] : "local";
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
   * Parses multiple templates out of a string in the format specified for {@link #parse(String)} split by ";".
   *
   * @param templates the templates in the specified format
   * @return an array of the parsed templates, this will not contain any null elements if any format is wrong
   */
  public static ServiceTemplate @NotNull [] parseArray(@NotNull String templates) {
    return Arrays.stream(templates.split(";"))
      .map(ServiceTemplate::parse)
      .filter(Objects::nonNull)
      .toArray(ServiceTemplate[]::new);
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull String getPrefix() {
    return this.prefix;
  }

  public @NotNull String getStorage() {
    return this.storage;
  }

  /**
   * This priority is used to determine in which order the template should be installed e.g. a Template with a priority
   * of 10 (high prio) is installed after Templates with a lower prio (e.g. 1) are installed to prevent that Templates
   * with lower priorities overwrite Templates with a higher one
   *
   * @return the priority of the template
   */
  public int getPriority() {
    return this.priority;
  }

  public boolean shouldAlwaysCopyToStaticServices() {
    return this.alwaysCopyToStaticServices;
  }

  public @NotNull String getFullName() {
    return this.prefix + '/' + this.name;
  }

  @Override
  public @NotNull String toString() {
    return this.storage + ':' + this.prefix + '/' + this.name;
  }

  /**
   * Creates a new {@link SpecificTemplateStorage} for this template.
   *
   * @return a new instance of the {@link SpecificTemplateStorage}
   * @throws IllegalArgumentException if the storage in this template doesn't exist
   */
  public @NotNull SpecificTemplateStorage storage() {
    return SpecificTemplateStorage.of(this);
  }

  /**
   * Creates a new {@link SpecificTemplateStorage} for the given template.
   *
   * @return a new instance of the {@link SpecificTemplateStorage} or null if the storage doesn't exist
   */
  public @Nullable SpecificTemplateStorage knownStorage() {
    var storage = CloudNetDriver.getInstance().getTemplateStorage(this.storage);
    return storage != null ? SpecificTemplateStorage.of(this, storage) : null;
  }

  @Override
  public @Range(from = -1, to = 1) int compareTo(@NotNull ServiceTemplate serviceTemplate) {
    return Integer.compare(this.priority, serviceTemplate.priority);
  }

  @Override
  public ServiceTemplate clone() {
    try {
      return (ServiceTemplate) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder {

    private String name;
    private String prefix;
    private String storage = ServiceTemplate.LOCAL_STORAGE;

    private int priority;
    private boolean alwaysCopyToStaticServices;

    public @NotNull Builder name(@NotNull String name) {
      this.name = name;
      return this;
    }

    public @NotNull Builder prefix(@NotNull String prefix) {
      this.prefix = prefix;
      return this;
    }

    public @NotNull Builder storage(@NotNull String storage) {
      this.storage = storage;
      return this;
    }

    public @NotNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public @NotNull Builder alwaysCopyToStaticServices(boolean alwaysCopyToStaticServices) {
      this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
      return this;
    }

    public @NotNull ServiceTemplate build() {
      Verify.verifyNotNull(this.name, "no name given");
      Verify.verifyNotNull(this.prefix, "no prefix given");

      return new ServiceTemplate(this.prefix, this.name, this.storage, this.priority, this.alwaysCopyToStaticServices);
    }
  }
}
