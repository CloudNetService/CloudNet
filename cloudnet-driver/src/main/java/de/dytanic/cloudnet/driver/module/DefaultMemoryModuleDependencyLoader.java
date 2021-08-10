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
import org.jetbrains.annotations.NotNull;

/**
 * A dependency loader which only creates a new url instance but will not save the dependency anywhere persistently.
 */
public class DefaultMemoryModuleDependencyLoader implements IModuleDependencyLoader {

  /**
   * Represents a maven dependency download url in the format: {@code <repo-url><group>/<name>/<version>/<name>-<version>.jar}.
   */
  protected static final String REMOTE_DEPENDENCY_URL_FORMAT = "%s%s/%s/%s/%s-%s.jar";

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull URL loadModuleDependencyByUrl(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) throws Exception {
    return new URL(dependency.getUrl());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull URL loadModuleDependencyByRepository(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency,
    @NotNull String repositoryUrl
  ) throws Exception {
    return new URL(String.format(
      REMOTE_DEPENDENCY_URL_FORMAT,
      repositoryUrl,
      dependency.getGroup().replace('.', '/'),
      dependency.getName(),
      dependency.getVersion(),
      dependency.getName(),
      dependency.getVersion()
    ));
  }
}
