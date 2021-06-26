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

public interface IModule {

  IModuleWrapper getModuleWrapper();

  ClassLoader getClassLoader();

  ModuleConfiguration getModuleConfig();

  default String getGroup() {
    return this.getModuleConfig().group;
  }

  default String getName() {
    return this.getModuleConfig().name;
  }

  default String getVersion() {
    return this.getModuleConfig().version;
  }

  default String getWebsite() {
    return this.getModuleConfig().website;
  }

  default String getDescription() {
    return this.getModuleConfig().description;
  }

}
