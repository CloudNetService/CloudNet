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

package eu.cloudnetservice.driver.module;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a dependency for the desired module. The module dependencies for a module are specified in the module
 * configuration.
 * <p>
 * The specified dependency in the module configuration has to match in the following parts:
 * <ul>
 *   <li>group
 *   <li>name
 *   <li>version
 * </ul>
 *
 * @see ModuleConfiguration
 * @since 4.0
 */
@EqualsAndHashCode
public class ModuleDependency {

  private final String repo;
  private final String url;
  private final String group;
  private final String name;
  private final String version;
  private final String checksum;

  /**
   * Creates a new module dependency instance. This dependency will require another module to be loaded before.
   *
   * @param group   the group of the dependency.
   * @param name    the name of the dependency.
   * @param version the version of the dependency.
   * @throws NullPointerException if group, name or version is null.
   */
  public ModuleDependency(@NonNull String group, @NonNull String name, @NonNull String version) {
    this(null, group, name, version);
  }

  /**
   * Creates a new module dependency instance. This dependency type will (when the repository is provided) require a
   * dependency from a remote repository which needs to be downloaded. If the repository is not provided (in this case
   * null) this dependency will require another module to be loaded before.
   *
   * @param repo    the repository in which this dependency is located.
   * @param group   the group of the dependency.
   * @param name    the name of the dependency.
   * @param version the version of the dependency.
   * @throws NullPointerException if group, name or version is null.
   */
  public ModuleDependency(@Nullable String repo, @NonNull String group, @NonNull String name, @NonNull String version) {
    this(repo, null, group, name, version, null);
  }

  /**
   * Creates a new module dependency instance. This dependency type will (when the repository is provided) require a
   * dependency from a remote repository which needs to be downloaded. If the direct download url is provided this
   * dependency will be loaded from the direct url. Please note: if both the repository and url is provided, the direct
   * download url will be ignored. If neither the repository nor the direct url is not provided (in this case null) this
   * dependency will require another module to be loaded before.
   *
   * @param repo     the repository in which this dependency is located.
   * @param url      the direct download url of this dependency.
   * @param group    the group of the dependency.
   * @param name     the name of the dependency.
   * @param version  the version of the dependency.
   * @param checksum the checksum of the dependency, null if this dependency represents a module.
   * @throws NullPointerException if group, name or version is null.
   */
  public ModuleDependency(
    @Nullable String repo,
    @Nullable String url,
    @NonNull String group,
    @NonNull String name,
    @NonNull String version,
    @Nullable String checksum
  ) {
    this.repo = repo;
    this.url = url;
    this.group = group;
    this.name = name;
    this.version = version;
    this.checksum = checksum;
  }

  /**
   * Get the repository this dependency is located in.
   *
   * @return the repository this dependency is located in or null if not located in a repository.
   */
  public @Nullable String repo() {
    return this.repo;
  }

  /**
   * Get the direct download url of this dependency.
   *
   * @return the direct download url of this dependency or null if there is no direct download url.
   */
  public @Nullable String url() {
    return this.url;
  }

  /**
   * Get the group of this dependency.
   *
   * @return the group of this dependency.
   */
  public @NonNull String group() {
    return this.group;
  }

  /**
   * Get the name of this dependency.
   *
   * @return the name of this dependency.
   */
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the version of this dependency.
   *
   * @return the version of this dependency.
   */
  public @NonNull String version() {
    return this.version;
  }

  /**
   * Get the checksum of this dependency, if known.
   *
   * @return the checksum of this dependency.
   */
  public @Nullable String checksum() {
    return this.checksum;
  }

  /**
   * Asserts that the required properties (group, name, version) are present.
   *
   * @throws NullPointerException if one of required properties is not set.
   */
  public void assertDefaultPropertiesSet() {
    Preconditions.checkNotNull(this.group, "Missing group of module dependency");
    Preconditions.checkNotNull(this.name, "Missing name of module dependency");
    Preconditions.checkNotNull(this.version, "Missing version of module dependency");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.group + ':' + this.name + ':' + this.version;
  }
}
