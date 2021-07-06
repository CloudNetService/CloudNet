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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface IModuleWrapper {

  EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks();

  IModule getModule();

  ModuleLifeCycle getModuleLifeCycle();

  IModuleProvider getModuleProvider();

  ModuleConfiguration getModuleConfiguration();

  JsonDocument getModuleConfigurationSource();

  ClassLoader getClassLoader();

  IModuleWrapper loadModule();

  IModuleWrapper startModule();

  IModuleWrapper stopModule();

  IModuleWrapper unloadModule();

  @Deprecated
  default File getDataFolder() {
    return this.getDataDirectory().toFile();
  }

  @NotNull
  Path getDataDirectory();

  Map<String, String> getDefaultRepositories();

}
