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

import com.google.common.base.VerifyException;
import org.jetbrains.annotations.NotNull;

// TODO: cleanup here
public interface IModule {

  /**
   * Initializes the module with the necessary information. This method can only be called once.
   *
   * @param loader  the class loader used to load all dependencies and the main class of the module.
   * @param wrapper the created module wrapper which wraps this module.
   * @param config  the deserialized module configuration located in the module file.
   * @throws VerifyException      if this module instance is already initialized.
   * @throws NullPointerException if the provided loader, wrapper or module config is null.
   */
  void init(@NotNull ClassLoader loader, @NotNull IModuleWrapper wrapper, @NotNull ModuleConfiguration config);

  /**
   * Get the module wrapper which is associated with this module.
   *
   * @return the module wrapper which is associated with this module.
   */
  @NotNull IModuleWrapper getModuleWrapper();

  /**
   * Get the class loader which is responsible for this module.
   *
   * @return the class loader which is responsible for this module.
   */
  @NotNull ClassLoader getClassLoader();

  /**
   * Get the module configuration which was deserialized based on the information located in the module.
   *
   * @return the module configuration located in this module file.
   */
  @NotNull ModuleConfiguration getModuleConfig();

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
