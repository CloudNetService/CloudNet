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
import lombok.NonNull;
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
  @NonNull
  @Unmodifiable Map<ModuleLifeCycle, List<IModuleTaskEntry>> moduleTasks();

  /**
   * Get all modules this module is depending on.
   *
   * @return an immutable set of all modules this module is depending on.
   */
  @NonNull
  @Unmodifiable Set<ModuleDependency> dependingModules();

  /**
   * Get the wrapped module instance of this wrapper.
   *
   * @return the wrapped module instance of this wrapper.
   */
  @NonNull IModule module();

  /**
   * Get the current lifecycle of this wrapper.
   *
   * @return the current lifecycle of this wrapper.
   */
  @NonNull ModuleLifeCycle moduleLifeCycle();

  /**
   * Get the module provider which loaded this module.
   *
   * @return the module provider which loaded this module.
   */
  @NonNull IModuleProvider moduleProvider();

  /**
   * Get the module configuration on which base the module was created.
   *
   * @return the module configuration on which base the module was created.
   */
  @NonNull ModuleConfiguration moduleConfiguration();

  /**
   * Get the class loader which is responsible for this module.
   *
   * @return the class loader which is responsible for this module.
   */
  @NonNull ClassLoader classLoader();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#LOADED} if possible and fires all associated tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #moduleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NonNull IModuleWrapper loadModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#STARTED} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #moduleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NonNull IModuleWrapper startModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#RELOADING} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #moduleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NonNull IModuleWrapper reloadModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#STOPPED} if possible and fires all associated
   * tasks.
   *
   * @return the same instance of this class, for chaining.
   * @see #moduleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NonNull IModuleWrapper stopModule();

  /**
   * Changes the lifecycle of this module to {@link ModuleLifeCycle#UNLOADED} if possible and fires all associated
   * tasks. The module will be unregistered from the provider, the class loader will be closed and the state of this
   * module changes to {@link ModuleLifeCycle#UNUSEABLE}.
   *
   * @return the same instance of this class, for chaining.
   * @see #moduleTasks()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see IModuleProvider#notifyPreModuleLifecycleChange(IModuleWrapper, ModuleLifeCycle)
   */
  @NonNull IModuleWrapper unloadModule();

  /**
   * Get the data directory of this module in which the module should store its configuration files.
   *
   * @return the data directory of this module.
   */
  @NonNull Path dataDirectory();

  /**
   * Get the url from where the module was loaded.
   *
   * @return the url from where the module was loaded.
   */
  @NonNull URL url();

  /**
   * Get the uri from where the module was loaded.
   *
   * @return the uri from where the module was loaded.
   */
  @NonNull URI uri();
}
