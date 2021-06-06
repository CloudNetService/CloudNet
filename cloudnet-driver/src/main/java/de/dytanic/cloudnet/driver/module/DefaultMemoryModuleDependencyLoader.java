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

public class DefaultMemoryModuleDependencyLoader implements IModuleDependencyLoader {

  @Override
  public URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency,
    Map<String, String> moduleRepositoriesUrls) throws Exception {
    return new URL(moduleDependency.getUrl());
  }

  @Override
  public URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration,
    ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception {
    return new URL(
      moduleRepositoriesUrls.get(moduleDependency.getRepo()) +
        moduleDependency.getGroup().replace(".", "/") + "/" +
        moduleDependency.getName() + "/" + moduleDependency.getVersion() + "/" +
        moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar"
    );
  }
}
