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

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public interface IModuleProvider {

  @Deprecated
  default File getModuleDirectory() {
    return this.getModuleDirectoryPath().toFile();
  }

  @Deprecated
  default void setModuleDirectory(File moduleDirectory) {
    this.setModuleDirectoryPath(moduleDirectory.toPath());
  }

  Path getModuleDirectoryPath();

  void setModuleDirectoryPath(Path moduleDirectory);

  IModuleProviderHandler getModuleProviderHandler();

  void setModuleProviderHandler(IModuleProviderHandler moduleProviderHandler);

  IModuleDependencyLoader getModuleDependencyLoader();

  void setModuleDependencyLoader(IModuleDependencyLoader moduleDependencyLoader);

  Collection<IModuleWrapper> getModules();

  Collection<IModuleWrapper> getModules(String group);

  IModuleWrapper getModule(String name);

  IModuleWrapper loadModule(URL url);

  IModuleWrapper loadModule(File file);

  IModuleWrapper loadModule(Path path);

  IModuleProvider loadModule(URL... urls);

  IModuleProvider loadModule(File... files);

  IModuleProvider loadModule(Path... paths);

  IModuleProvider startAll();

  IModuleProvider stopAll();

  IModuleProvider unloadAll();

}
