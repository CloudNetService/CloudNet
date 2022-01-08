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

package eu.cloudnetservice.cloudnet.driver.module;

import java.net.URL;
import lombok.NonNull;

/**
 * A loader for dependencies which can be provided by a module.
 */
public interface ModuleDependencyLoader {

  /**
   * Provides an url for the fixed download url provided in the module dependency.
   *
   * @param configuration the module configuration associated with the module which loads the dependency.
   * @param dependency    the dependency will should be converted to a download url.
   * @return an url targeting the source of the dependency from which it can be loaded.
   * @throws Exception            if any exception occurs during the load of the dependency.
   * @throws NullPointerException if configuration or dependency is null.
   * @see ModuleDependency#url()
   */
  @NonNull URL loadModuleDependencyByUrl(
    @NonNull ModuleConfiguration configuration, @NonNull ModuleDependency dependency) throws Exception;

  /**
   * Provides an url from which the provided dependency can be loaded.
   *
   * @param configuration the module configuration associated with the module which loads the dependency.
   * @param dependency    the dependency will should be converted to a download url.
   * @param repositoryUrl the repository which is associated with the dependency.
   * @return an url targeting the source of the dependency from which it can be loaded.
   * @throws Exception            if any exception occurs during the load of the dependency.
   * @throws NullPointerException if configuration, dependency or {@code repositoryUrl} is null.
   */
  @NonNull URL loadModuleDependencyByRepository(@NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency, @NonNull String repositoryUrl) throws Exception;
}
