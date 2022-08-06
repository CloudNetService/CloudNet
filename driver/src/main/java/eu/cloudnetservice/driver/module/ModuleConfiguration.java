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
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import java.nio.file.Path;
import lombok.NonNull;
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
 * @param runtimeModule       whether this module is a runtime module. Runtime modules are not reloadable and any
 *                            updates require a cloudnet restart.
 * @param storesSensitiveData whether this module stores sensitive data. If true the module configuration will not be
 *                            dumped to the paste command output.
 * @param group               the group id of this module.
 * @param name                the name of this module.
 * @param version             the version of this module.
 * @param main                the main entry-point of this module.
 * @param description         the description of this module.
 * @param author              the developer and author of this module.
 * @param website             the website associated with this module.
 * @param dataFolder          the data folder of the module - null results in the default folder structure.
 * @param repositories        all defined repositories of this module or null if no repositories are defined.
 * @param dependencies        all defined dependencies of this module or null if no dependencies are defined.
 * @param minJavaVersionId    the minimum required java version for this module to work.
 * @param properties          any extra properties for this module.
 * @see ModuleConfigurationNotFoundException
 * @since 4.0
 */
public record ModuleConfiguration(
  boolean runtimeModule,
  boolean storesSensitiveData,
  @NonNull String group,
  @NonNull String name,
  @NonNull String version,
  @NonNull String main,
  @Nullable String description,
  @Nullable String author,
  @Nullable String website,
  @Nullable String dataFolder,
  @Nullable ModuleRepository[] repositories,
  @Nullable ModuleDependency[] dependencies,
  int minJavaVersionId,
  @Nullable JsonDocument properties
) implements Nameable {

  /**
   * Get the data folder path of this module or a default version of it based on the module's name.
   *
   * @param baseDirectory the base directory of the module provider used to resolve the data folder if no data folder is
   *                      specified explicitly.
   * @return the data folder in path form of this module.
   */
  public @NonNull Path dataFolder(@NonNull Path baseDirectory) {
    if (this.dataFolder == null) {
      // default data folder name based on the module name
      return baseDirectory.resolve(this.name);
    } else {
      // get the data folder of this module from the root directory of the cloud
      return FileUtil.resolve(Path.of(""), this.dataFolder.split("/"));
    }
  }

  /**
   * Get the minimum java runtime version this module can run on or null if all runtime versions are supported.
   *
   * @return the minimum java runtime version this module can run on.
   */
  public @Nullable JavaVersion minJavaVersion() {
    return this.minJavaVersionId > 0 ? JavaVersion.guessFromMajor(this.minJavaVersionId) : null;
  }

  /**
   * Checks if this module can run on the specified java runtime version.
   *
   * @param javaVersion the runtime version to check.
   * @return if this module can run on the specified java version.
   */
  public boolean canRunOn(@NonNull JavaVersion javaVersion) {
    var minJavaVersion = this.minJavaVersion();
    return minJavaVersion == null || !minJavaVersion.supported() || javaVersion.isNewerOrAt(minJavaVersion);
  }
}
