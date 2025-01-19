/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.module;

import lombok.NonNull;

/**
 * The module provider handler handles the notifications for all module lifecycles changes for all loaded modules.
 *
 * @see ModuleLifeCycle
 * @see ModuleWrapper
 * @see ModuleProvider
 * @since 4.0
 */
public interface ModuleProviderHandler {

  /**
   * Called when a module is about to get loaded.
   *
   * @param moduleWrapper the module wrapper which will be loaded.
   * @return if the module is allowed to load.
   * @throws NullPointerException if the given module wrapper is null.
   */
  boolean handlePreModuleLoad(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module was loaded.
   *
   * @param moduleWrapper the module which was loaded.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePostModuleLoad(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get started.
   *
   * @param moduleWrapper the module which will be started.
   * @return if the module is allowed to start.
   * @throws NullPointerException if the given module wrapper is null.
   */
  boolean handlePreModuleStart(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module was started.
   *
   * @param moduleWrapper the module which was started.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePostModuleStart(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get reloaded.
   *
   * @param moduleWrapper the module which will be reloaded.
   * @return if the module is allowed to be reloaded.
   * @throws NullPointerException if the given module wrapper is null.
   */
  boolean handlePreModuleReload(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module was reloaded.
   *
   * @param moduleWrapper the module which was reloaded.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePostModuleReload(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get stopped.
   *
   * @param moduleWrapper the module which will be stopped.
   * @return if the module is allowed to stop.
   * @throws NullPointerException if the given module wrapper is null.
   */
  boolean handlePreModuleStop(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module was stopped.
   *
   * @param moduleWrapper the module which was stopped.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePostModuleStop(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module is about to get unloaded.
   *
   * @param moduleWrapper the module which will be unloaded.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePreModuleUnload(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a module was unloaded.
   *
   * @param moduleWrapper the module which was unloaded.
   * @throws NullPointerException if the given module wrapper is null.
   */
  void handlePostModuleUnload(@NonNull ModuleWrapper moduleWrapper);

  /**
   * Called when a dependency for a module is about to get loaded.
   *
   * @param configuration the configuration of the module in which the dependency is declared.
   * @param dependency    the dependency which will be loaded.
   * @throws NullPointerException if configuration or dependency is null.
   */
  void handlePreInstallDependency(@NonNull ModuleConfiguration configuration, @NonNull ModuleDependency dependency);

  /**
   * Called when a dependency for a module was loaded.
   *
   * @param configuration the configuration of the module in which the dependency is declared.
   * @param dependency    the dependency which was loaded.
   * @throws NullPointerException if configuration or dependency is null.
   */
  void handlePostInstallDependency(@NonNull ModuleConfiguration configuration, @NonNull ModuleDependency dependency);
}
