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

import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import kong.unirest.Unirest;
import lombok.NonNull;

/**
 * A dependency loader which will download and save the provided module dependencies persistently on the local file
 * system.
 */
public class DefaultModuleDependencyLoader implements ModuleDependencyLoader {

  /**
   * A format for the file name with which the module will be stored: {@code <name>-<version>.jar}
   */
  protected static final String FILE_NAME_FORMAT = "%s-%s.jar";
  /**
   * Represents a maven dependency download url in the format: {@code <repo-url><group>/<name>/<version>/<name>-<version>.jar}.
   */
  protected static final String REMOTE_DEPENDENCY_URL_FORMAT = "%s%s/%s/%s/%s-%s.jar";

  protected final Path baseDirectory;

  /**
   * Constructs a new instance of this class.
   *
   * @param baseDirectory the base directory in which the dependencies should be stored.
   */
  public DefaultModuleDependencyLoader(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull URL loadModuleDependencyByUrl(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) throws Exception {
    var url = Objects.requireNonNull(dependency.url(), "Dependency url must be given");
    return this.loadDependency(dependency, new URL(url));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull URL loadModuleDependencyByRepository(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency,
    @NonNull String repositoryUrl
  ) throws Exception {
    return this.loadDependency(
      dependency,
      new URL(String.format(
        REMOTE_DEPENDENCY_URL_FORMAT,
        repositoryUrl,
        dependency.group().replace('.', '/'),
        dependency.name(),
        dependency.version(),
        dependency.name(),
        dependency.version())));
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
  protected @NonNull URL loadDependency(@NonNull ModuleDependency dependency, @NonNull URL url) throws Exception {
    var destFile = FileUtils.resolve(this.baseDirectory, dependency.group().split("\\."))
      .resolve(dependency.name())
      .resolve(dependency.version())
      .resolve(String.format(FILE_NAME_FORMAT, dependency.name(), dependency.version()));
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
  public @NonNull Path baseDirectory() {
    return this.baseDirectory;
  }
}
