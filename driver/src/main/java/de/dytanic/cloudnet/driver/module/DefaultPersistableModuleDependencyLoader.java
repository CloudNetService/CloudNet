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

import de.dytanic.cloudnet.common.io.FileUtils;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import kong.unirest.Unirest;
import org.jetbrains.annotations.NotNull;

/**
 * A dependency loader which will download and save the provided module dependencies persistently on the local file
 * system.
 */
public class DefaultPersistableModuleDependencyLoader extends DefaultMemoryModuleDependencyLoader {

  /**
   * A format for the file name with which the module will be stored: {@code <name>-<version>.jar}
   */
  protected static final String FILE_NAME_FORMAT = "%s-%s.jar";

  protected final Path baseDirectory;

  /**
   * Constructs a new instance of this class.
   *
   * @param baseDirectory the base directory in which the dependencies should be stored.
   */
  public DefaultPersistableModuleDependencyLoader(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    FileUtils.createDirectory(baseDirectory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull URL loadModuleDependencyByUrl(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) throws Exception {
    URL memoryBasedUrl = super.loadModuleDependencyByUrl(configuration, dependency);
    return this.loadDependency(dependency, memoryBasedUrl);
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
    URL memoryBasedUrl = super.loadModuleDependencyByRepository(configuration, dependency, repositoryUrl);
    return this.loadDependency(dependency, memoryBasedUrl);
  }

  /**
   * Loads and stores a dependency on the local file system. This method will not override existing versions of the
   * file.
   *
   * @param dependency the dependency which gets loaded.
   * @param url        the url from where the dependency should be loaded.
   * @return the url to the file on the local file system after the load.
   * @throws Exception if any exception occurs during the load of the dependency.
   */
  protected @NotNull URL loadDependency(@NotNull ModuleDependency dependency, @NotNull URL url) throws Exception {
    Path destFile = FileUtils.resolve(this.baseDirectory, dependency.getGroup().split("\\."))
      .resolve(dependency.getName())
      .resolve(dependency.getVersion())
      .resolve(String.format(FILE_NAME_FORMAT, dependency.getName(), dependency.getVersion()));
    FileUtils.ensureChild(this.baseDirectory, destFile);

    if (Files.notExists(destFile)) {
      Files.createDirectories(destFile.getParent());

      Unirest.get(url.toExternalForm()).asFile(destFile.toString());
    }

    return destFile.toUri().toURL();
  }

  /**
   * Get the base directory in which the dependencies should be stored.
   *
   * @return the base directory in which the dependencies should be stored.
   */
  public @NotNull Path getBaseDirectory() {
    return this.baseDirectory;
  }
}
