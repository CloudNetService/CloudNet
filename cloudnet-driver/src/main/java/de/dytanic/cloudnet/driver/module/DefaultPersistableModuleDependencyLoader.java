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
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DefaultPersistableModuleDependencyLoader implements IModuleDependencyLoader {

  private static final Logger LOGGER = LogManager.getLogger(DefaultPersistableModuleDependencyLoader.class);

  protected final Path baseDirectory;

  public DefaultPersistableModuleDependencyLoader(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    try {
      Files.createDirectories(baseDirectory);
    } catch (IOException exception) {
      LOGGER.severe("Exception while creating directories", exception);
    }
  }

  @Override
  public URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency,
    Map<String, String> moduleRepositoriesUrls) throws Exception {
    return this.loadModuleDependency0(moduleDependency, moduleDependency.getUrl());
  }

  @Override
  public URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration,
    ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception {
    return this.loadModuleDependency0(moduleDependency, moduleRepositoriesUrls.get(moduleDependency.getRepo()) +
      moduleDependency.getGroup().replace(".", "/") + "/" +
      moduleDependency.getName() + "/" + moduleDependency.getVersion() + "/" +
      moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar");
  }

  private URL loadModuleDependency0(ModuleDependency moduleDependency, String url) throws Exception {
    Path destFile = this.baseDirectory
      .resolve(moduleDependency.getGroup().replace(".", "/") + "/" + moduleDependency.getName() +
        "/" + moduleDependency.getVersion() + "/" + moduleDependency.getName() + "-" + moduleDependency.getVersion()
        + ".jar");

    if (!Files.exists(destFile)) {
      Files.createDirectories(destFile.getParent());

      HttpURLConnection urlConnection = HttpConnectionProvider.provideConnection(url);
      urlConnection.setUseCaches(false);
      urlConnection.connect();

      try (InputStream inputStream = urlConnection.getInputStream()) {
        Files.copy(inputStream, destFile);
      }
    }

    return destFile.toUri().toURL();
  }

  public Path getBaseDirectory() {
    return this.baseDirectory;
  }
}
