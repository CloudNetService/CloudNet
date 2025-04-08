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
import eu.cloudnetservice.driver.base.Named;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a repository for a module dependency, the dependency is locatable at this repository.
 *
 * @see ModuleDependency
 * @see ModuleConfiguration
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class ModuleRepository implements Named {

  private String name;
  private String url;

  /**
   * Constructs a new instance of this class.
   *
   * @param name the name of the repository, must be unique. Duplicate names will override each other.
   * @param url  the url of the repository.
   * @throws NullPointerException if name or url is null
   */
  public ModuleRepository(@NonNull String name, @NonNull String url) {
    this.name = name;
    this.url = url;
  }

  /**
   * This constructor is for internal use only. The name and url of this repository is required. Creating a repository
   * using this constructor will cause an exception when loading the module. See {@link #assertComplete()}.
   */
  @ApiStatus.Internal
  public ModuleRepository() {
  }

  /**
   * Get the name of this repository.
   *
   * @return the name of this repository.
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the url of this repository.
   *
   * @return the url of this repository.
   */
  public @NonNull String url() {
    return this.url;
  }

  /**
   * Asserts that the required properties (name, url) are present.
   *
   * @throws NullPointerException if one of required properties is not set.
   */
  public void assertComplete() {
    Preconditions.checkNotNull(this.name, "Missing repository name");
    Preconditions.checkNotNull(this.url, "Missing repository url");
  }
}
