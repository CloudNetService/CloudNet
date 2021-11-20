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

import org.jetbrains.annotations.NotNull;

/**
 * Represents a handler for module provider actions.
 */
public interface IModuleProviderHandler {

  /**
   * Called when a module is about to get loaded.
   *
   * @param moduleWrapper the module wrapper which will be loaded.
   * @return if the module is allowed to load.
   */
  boolean handlePreModuleLoad(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module was loaded.
   *
   * @param moduleWrapper the module which was loaded.
   */
  void handlePostModuleLoad(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get started.
   *
   * @param moduleWrapper the module which will be started.
   * @return if the module is allowed to start.
   */
  boolean handlePreModuleStart(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module was started.
   *
   * @param moduleWrapper the module which was started.
   */
  void handlePostModuleStart(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get reloaded.
   *
   * @param moduleWrapper the module which will be reloaded.
   * @return if the module is allowed to be reloaded.
   */
  boolean handlePreModuleReload(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module was reloaded.
   *
   * @param moduleWrapper the module which was reloaded.
   */
  void handlePostModuleReload(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get stopped.
   *
   * @param moduleWrapper the module which will be stopped.
   * @return if the module is allowed to stop.
   */
  boolean handlePreModuleStop(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module was stopped.
   *
   * @param moduleWrapper the module which was stopped.
   */
  void handlePostModuleStop(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get unloaded.
   *
   * @param moduleWrapper the module which will be unloaded.
   */
  void handlePreModuleUnload(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a module was unloaded.
   *
   * @param moduleWrapper the module which was unloaded.
   */
  void handlePostModuleUnload(@NotNull IModuleWrapper moduleWrapper);

  /**
   * Called when a dependency for a module is about to get loaded.
   *
   * @param configuration the configuration of the module in which the dependency is declared.
   * @param dependency    the dependency which will be loaded.
   */
  void handlePreInstallDependency(@NotNull ModuleConfiguration configuration, @NotNull ModuleDependency dependency);

  /**
   * Called when a dependency for a module was loaded.
   *
   * @param configuration the configuration of the module in which the dependency is declared.
   * @param dependency    the dependency which was loaded.
   */
  void handlePostInstallDependency(@NotNull ModuleConfiguration configuration, @NotNull ModuleDependency dependency);
}
