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

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a wrapper for a module.
 */
public interface IModuleWrapper {

  /**
   * Get all module tasks which were detected in the main class of the module.
   *
   * @return an immutable map of all module tasks which were detected in the main class of the module.
   */
  @NotNull
  @Unmodifiable Map<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks();

  /**
   * Get all modules this module is depending on.
   *
   * @return an immutable set of all modules this module is depending on.
   */
  @NotNull
  @Unmodifiable Set<ModuleDependency> getDependingModules();

  /**
   * Get the wrapped module instance of this wrapper.
   *
   * @return the wrapped module instance of this wrapper.
   */
  @NotNull IModule getModule();

  /**
   * Get the current lifecycle of this wrapper.
   *
   * @return the current lifecycle of this wrapper.
   */
  @NotNull ModuleLifeCycle getModuleLifeCycle();

  /**
   * Get the module provider which loaded this module.
   *
   * @return the module provider which loaded this module.
   */
  @NotNull IModuleProvider getModuleProvider();

  /**
   * Get the module configuration on which base the module was created.
   *
   * @return the module configuration on which base the module was created.
   */
  @NotNull ModuleConfiguration getModuleConfiguration();

  /**
   * Get the class loader which is responsible for this module.
   *
   * @return the class loader which is responsible for this module.
   */
  @NotNull ClassLoader getClassLoader();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#LOADED} if possible and fires all associated tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #getModuleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NotNull IModuleWrapper loadModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#STARTED} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #getModuleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NotNull IModuleWrapper startModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#RELOADING} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #getModuleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NotNull IModuleWrapper reloadModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#STOPPED} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #getModuleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NotNull IModuleWrapper stopModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#UNLOADED} if possible and fires all associated
   * tasks. The module will be unregistered from the provider, the class loader will be closed and the state of this
   * module changes to {@link ModuleLifeCycle#UNUSEABLE}.
   *
   * @return the same instance of this class, for chaining.
   * @see #getModuleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NotNull IModuleWrapper unloadModule();

  /**
   * Get the data directory of this module in which the module should store its configuration files.
   *
   * @return the data directory of this module.
   */
  @NotNull Path getDataDirectory();

  /**
   * Get the url from where the module was loaded.
   *
   * @return the url from where the module was loaded.
   */
  @NotNull URL getUrl();

  /**
   * Get the uri from where the module was loaded.
   *
   * @return the uri from where the module was loaded.
   */
  @NotNull URI getUri();
}
