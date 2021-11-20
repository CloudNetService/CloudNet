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

package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a deserialized form of a module.json file.
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
    @NotNull String group,
    @NotNull String name,
    @NotNull String version,
    @NotNull String main
  ) {
    this.group = Preconditions.checkNotNull(group, "group cannot be null");
    this.name = Preconditions.checkNotNull(name, "name cannot be null");
    this.version = Preconditions.checkNotNull(version, "version cannot be null");
    this.main = Preconditions.checkNotNull(main, "main cannot be null");
  }

  @Internal
  public ModuleConfiguration(
    boolean runtimeModule,
    boolean storesSensitiveData,
    @NotNull String group,
    @NotNull String name,
    @NotNull String version,
    @NotNull String main,
    @NotNull String description,
    @NotNull String author,
    @NotNull String website,
    @NotNull String dataFolder,
    @NotNull ModuleRepository[] repositories,
    @NotNull ModuleDependency[] dependencies,
    int minJavaVersionId,
    @NotNull JsonDocument properties
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
  public boolean isRuntimeModule() {
    return this.runtimeModule;
  }

  /**
   * Get if this module stores sensitive data. If {@code true} the module configuration will not be dumped to the paste
   * command output.
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
  public @NotNull String getGroup() {
    return this.group;
  }

  /**
   * Get the name of this module.
   *
   * @return the name of this module.
   */
  public @NotNull String getName() {
    return this.name;
  }

  /**
   * Get the version of this module.
   *
   * @return the version of this module.
   */
  public @NotNull String getVersion() {
    return this.version;
  }

  /**
   * Get the main class of this module.
   *
   * @return the main class of this module.
   */
  public @NotNull String getMainClass() {
    return this.main;
  }

  /**
   * Get the description of this module or {@code null} if no description is provided.
   *
   * @return the description of this module.
   */
  public @Nullable String getDescription() {
    return this.description;
  }

  /**
   * Get the author of this module or {@code null} if no author is defined.
   *
   * @return the author of this module.
   */
  public @Nullable String getAuthor() {
    return this.author;
  }

  /**
   * Get the website of this module or {@code null} if no website is defined.
   *
   * @return the website of this module.
   */
  public @Nullable String getWebsite() {
    return this.website;
  }

  /**
   * Get the data folder of this module or {@code null} if no data folder is defined.
   *
   * @return the data folder of this module.
   */
  public @Nullable String getDataFolder() {
    return this.dataFolder;
  }

  /**
   * Get the data folder path of this module or a default version of it based on the module's name.
   *
   * @param moduleProviderBaseDirectory the base directory of the module provider used to resolve the data folder if no
   *                                    data folder is specified explicitly.
   * @return the data folder in path form of this module.
   */
  public @NotNull Path getDataFolder(@NotNull Path moduleProviderBaseDirectory) {
    if (this.dataFolder == null) {
      // default data folder name based on the module name
      return moduleProviderBaseDirectory.resolve(this.name);
    } else {
      // get the data folder of this module from the root directory of the cloud
      return FileUtils.resolve(Paths.get(""), this.dataFolder.split("/"));
    }
  }

  /**
   * Get all defined repositories of this module or {@code null} if no repositories are defined.
   *
   * @return all defined repositories of this module.
   */
  public @Nullable ModuleRepository[] getRepositories() {
    return this.repositories;
  }

  /**
   * Get all defined dependencies of this module or {@code null} if no dependencies are defined.
   *
   * @return all defined dependencies of this module.
   */
  public @Nullable ModuleDependency[] getDependencies() {
    return this.dependencies;
  }

  /**
   * Get the properties of this module or {@code null} if no properties are defined.
   *
   * @return all defined properties of this module.
   */
  public @Nullable JsonDocument getProperties() {
    return this.properties;
  }

  /**
   * Get the min java version number this module can run on. For example if the module can run on java 11+ this method
   * will return {@code 11}.
   *
   * @return the min java version number this module can run on.
   */
  public int getMinJavaVersionId() {
    return this.minJavaVersionId;
  }

  /**
   * Get the minimum java runtime version this module can run on or {@code null} if all runtime versions are supported.
   *
   * @return the minimum java runtime version this module can run on.
   */
  public @Nullable JavaVersion getMinJavaVersion() {
    return JavaVersion.fromVersion(this.minJavaVersionId).orElse(null);
  }

  /**
   * Checks if this module can run on the specified java runtime version.
   *
   * @param javaVersion the runtime version to check.
   * @return if this module can run on the specified java version.
   */
  public boolean canRunOn(@NotNull JavaVersion javaVersion) {
    JavaVersion minJavaVersion = this.getMinJavaVersion();
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
  protected void validatePropertyNotNull(@Nullable String field, @NotNull String fieldName) {
    if (field == null || field.trim().isEmpty()) {
      throw new ModuleConfigurationPropertyNotFoundException(fieldName);
    }
  }
}
