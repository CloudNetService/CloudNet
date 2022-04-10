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

package eu.cloudnetservice.driver.module;

import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the module.json file that each module.jar has to contain. The configuration requires the following
 * properties:
 * <ul>
 *   <li>group</li>
 *   <li>name</li>
 *   <li>version</li>
 *   <li>main</li>
 * </ul>
 * <p>
 * All required dependencies of a module should be listed in the {@link #dependencies()}
 * instead of shading them into the module jar.
 * <p>
 * If a dependency is not located in maven central the repo of the module dependency needs to be specified.
 *
 * @see ModuleConfigurationNotFoundException
 * @see ModuleConfigurationPropertyNotFoundException
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class ModuleConfiguration {

  protected boolean runtimeModule;
  protected boolean storesSensitiveData;

  protected String group;
  protected String name;
  protected String version;
  protected String main;

  protected String description;
  protected String author;
  protected String website;
  protected String dataFolder;

  protected ModuleRepository[] repositories;
  protected ModuleDependency[] dependencies;

  protected int minJavaVersionId;
  protected JsonDocument properties;

  @Internal
  public ModuleConfiguration() {
  }

  public ModuleConfiguration(
    @NonNull String group,
    @NonNull String name,
    @NonNull String version,
    @NonNull String main
  ) {
    this.group = group;
    this.name = name;
    this.version = version;
    this.main = main;
  }

  @Internal
  public ModuleConfiguration(
    boolean runtimeModule,
    boolean storesSensitiveData,
    @NonNull String group,
    @NonNull String name,
    @NonNull String version,
    @NonNull String main,
    @NonNull String description,
    @NonNull String author,
    @NonNull String website,
    @Nullable String dataFolder,
    @Nullable ModuleRepository[] repositories,
    @Nullable ModuleDependency[] dependencies,
    int minJavaVersionId,
    @Nullable JsonDocument properties
  ) {
    this.runtimeModule = runtimeModule;
    this.storesSensitiveData = storesSensitiveData;
    this.group = group;
    this.name = name;
    this.version = version;
    this.main = main;
    this.description = description;
    this.author = author;
    this.website = website;
    this.dataFolder = dataFolder;
    this.repositories = repositories;
    this.dependencies = dependencies;
    this.minJavaVersionId = minJavaVersionId;
    this.properties = properties;
  }

  /**
   * Get if this module is a runtime module. Runtime modules cannot be reloaded, updating their configuration requires a
   * cloudnet restart.
   *
   * @return if this module is a runtime module
   */
  public boolean runtimeModule() {
    return this.runtimeModule;
  }

  /**
   * Get if this module stores sensitive data. If true the module configuration will not be dumped to the paste command
   * output.
   *
   * @return if this module stores sensitive data.
   */
  public boolean storesSensitiveData() {
    return this.storesSensitiveData;
  }

  /**
   * Get the group id of this module.
   *
   * @return the group id of this module.
   */
  public @NonNull String group() {
    return this.group;
  }

  /**
   * Get the name of this module.
   *
   * @return the name of this module.
   */
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the version of this module.
   *
   * @return the version of this module.
   */
  public @NonNull String version() {
    return this.version;
  }

  /**
   * Get the main class of this module.
   *
   * @return the main class of this module.
   */
  public @NonNull String mainClass() {
    return this.main;
  }

  /**
   * Get the description of this module
   *
   * @return the description of this module.
   */
  public @NonNull String description() {
    return this.description;
  }

  /**
   * Get the author of this module or null if no author is defined.
   *
   * @return the author of this module.
   */
  public @NonNull String author() {
    return this.author;
  }

  /**
   * Get the website of this module
   *
   * @return the website of this module.
   */
  public @NonNull String website() {
    return this.website;
  }

  /**
   * Get the data folder of this module
   *
   * @return the data folder of this module.
   */
  public @Nullable String dataFolder() {
    return this.dataFolder;
  }

  /**
   * Get the data folder path of this module or a default version of it based on the module's name.
   *
   * @param moduleProviderBaseDirectory the base directory of the module provider used to resolve the data folder if no
   *                                    data folder is specified explicitly.
   * @return the data folder in path form of this module.
   */
  public @NonNull Path dataFolder(@NonNull Path moduleProviderBaseDirectory) {
    if (this.dataFolder == null) {
      // default data folder name based on the module name
      return moduleProviderBaseDirectory.resolve(this.name);
    } else {
      // get the data folder of this module from the root directory of the cloud
      return FileUtil.resolve(Path.of(""), this.dataFolder.split("/"));
    }
  }

  /**
   * Get all defined repositories of this module or null if no repositories are defined.
   *
   * @return all defined repositories of this module.
   */
  public @Nullable ModuleRepository[] repositories() {
    return this.repositories;
  }

  /**
   * Get all defined dependencies of this module or null if no dependencies are defined.
   *
   * @return all defined dependencies of this module.
   */
  public @Nullable ModuleDependency[] dependencies() {
    return this.dependencies;
  }

  /**
   * Get the properties of this module or null if no properties are defined.
   *
   * @return all defined properties of this module.
   */
  public @Nullable JsonDocument properties() {
    return this.properties;
  }

  /**
   * Get the min java version number this module can run on. For example if the module can run on java 11+ this method
   * will return 11.
   *
   * @return the min java version number this module can run on.
   */
  public int minJavaVersionId() {
    return this.minJavaVersionId;
  }

  /**
   * Get the minimum java runtime version this module can run on or null if all runtime versions are supported.
   *
   * @return the minimum java runtime version this module can run on.
   */
  public @Nullable JavaVersion minJavaVersion() {
    return JavaVersion.fromVersion(this.minJavaVersionId).orElse(null);
  }

  /**
   * Checks if this module can run on the specified java runtime version.
   *
   * @param javaVersion the runtime version to check.
   * @return if this module can run on the specified java version.
   */
  public boolean canRunOn(@NonNull JavaVersion javaVersion) {
    var minJavaVersion = this.minJavaVersion();
    return minJavaVersion == null || minJavaVersion.isSupportedByMax(javaVersion);
  }

  /**
   * Validates that all required configuration properties (group, name, version, main) are set.
   *
   * @throws ModuleConfigurationPropertyNotFoundException if one of the required properties is not set.
   */
  public void assertRequiredPropertiesSet() {
    this.validatePropertyNotNull(this.group, "group");
    this.validatePropertyNotNull(this.name, "name");
    this.validatePropertyNotNull(this.version, "version");
    this.validatePropertyNotNull(this.main, "main");
  }

  /**
   * Validates that the given property is present (not null and not empty).
   *
   * @param field     the actual field value which should be checked.
   * @param fieldName the name of the field which gets checked.
   * @throws ModuleConfigurationPropertyNotFoundException if the given field value is null or empty.
   */
  protected void validatePropertyNotNull(@Nullable String field, @NonNull String fieldName) {
    if (field == null || field.trim().isEmpty()) {
      throw new ModuleConfigurationPropertyNotFoundException(fieldName);
    }
  }
}
