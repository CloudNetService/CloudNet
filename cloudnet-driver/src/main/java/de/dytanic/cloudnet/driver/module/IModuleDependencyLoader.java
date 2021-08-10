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

import java.net.URL;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

/**
 * A loader for dependencies which can be provided by a module.
 */
public interface IModuleDependencyLoader {

  /**
   * Provides an url for the fixed download url provided in the module dependency.
   *
   * @param configuration the module configuration associated with the module which loads the dependency.
   * @param dependency    the dependency will should be converted to a download url.
   * @return an url targeting the source of the dependency from which it can be loaded.
   * @throws Exception if any exception occurs during the load of the dependency.
   * @see ModuleDependency#getUrl()
   */
  @NotNull URL loadModuleDependencyByUrl(
    @NotNull ModuleConfiguration configuration, @NotNull ModuleDependency dependency) throws Exception;

  /**
   * @deprecated Use {@link #loadModuleDependencyByUrl(ModuleConfiguration, ModuleDependency)} instead.
   */
  @Deprecated
  @ScheduledForRemoval
  default URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency,
    Map<String, String> moduleRepositoriesUrls) throws Exception {
    return this.loadModuleDependencyByUrl(moduleConfiguration, moduleDependency);
  }

  /**
   * Provides an url from which the provided dependency can be loaded.
   *
   * @param configuration the module configuration associated with the module which loads the dependency.
   * @param dependency    the dependency will should be converted to a download url.
   * @param repositoryUrl the repository which is associated with the dependency.
   * @return an url targeting the source of the dependency from which it can be loaded.
   * @throws Exception if any exception occurs during the load of the dependency.
   */
  @NotNull URL loadModuleDependencyByRepository(@NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency, @NotNull String repositoryUrl) throws Exception;

  /**
   * @deprecated Use {@link #loadModuleDependencyByRepository(ModuleConfiguration, ModuleDependency, String)} instead.
   */
  @Deprecated
  @ScheduledForRemoval
  default URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration,
    ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception {
    return this.loadModuleDependencyByRepository(moduleConfiguration, moduleDependency,
      moduleRepositoriesUrls.get(moduleDependency.getRepo()));
  }
}
