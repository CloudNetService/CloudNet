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
import java.nio.file.Path;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a loader and holder of modules.
 */
public interface IModuleProvider {

  /**
   * Get the base directory of this module provider. It will be used to provide the data directory for modules which did
   * not specifically set a data directory.
   *
   * @return the base directory of this module provider.
   * @see #setModuleDirectoryPath(Path)
   */
  @NotNull Path getModuleDirectoryPath();

  /**
   * Sets the base directory of this module provider. It will be used to provide the data directory for modules which
   * did not specifically set a data directory.
   *
   * @param moduleDirectory the module directory to use.
   * @see #getModuleDirectoryPath()
   */
  void setModuleDirectoryPath(@NotNull Path moduleDirectory);

  /**
   * Get the module provider handler of this provider or {@code null} when no handler is specified.
   *
   * @return the module provider handler of this provider or {@code null}.
   * @see #setModuleProviderHandler(IModuleProviderHandler)
   */
  @Nullable IModuleProviderHandler getModuleProviderHandler();

  /**
   * Sets the module provider handler of this provider.
   *
   * @param moduleProviderHandler the new module provider to use or {@code null} when no handler should be used.
   * @see #getModuleProviderHandler()
   */
  void setModuleProviderHandler(@Nullable IModuleProviderHandler moduleProviderHandler);

  /**
   * Get the module dependency loader. It's used to load all dependencies of all modules.
   * <p>This handler is by default {@link DefaultMemoryModuleDependencyLoader}.</p>
   *
   * @return the module provider handler used for this provider.
   * @see #setModuleDependencyLoader(IModuleDependencyLoader)
   * @see DefaultMemoryModuleDependencyLoader
   * @see DefaultPersistableModuleDependencyLoader
   */
  @NotNull IModuleDependencyLoader getModuleDependencyLoader();

  /**
   * Sets the module dependency loader which should be used by this provider.
   *
   * @param moduleDependencyLoader the module dependency loader to use.
   * @see #getModuleDependencyLoader()
   * @see DefaultMemoryModuleDependencyLoader
   * @see DefaultPersistableModuleDependencyLoader
   */
  void setModuleDependencyLoader(@NotNull IModuleDependencyLoader moduleDependencyLoader);

  /**
   * Get all loaded, started, stopped modules provided by this provider.
   *
   * @return an immutable set of all loaded, started, stopped modules provided by this provider.
   * @see ModuleLifeCycle
   */
  @NotNull
  @Unmodifiable Collection<IModuleWrapper> getModules();

  /**
   * Get all loaded, started, stopped modules provided by this provider which have the specific given group.
   *
   * @param group the group id of the modules to get.
   * @return an immutable set of all loaded, started, stopped modules provided by this provider which have the specific
   * given group.
   * @see ModuleLifeCycle
   */
  @NotNull
  @Unmodifiable Collection<IModuleWrapper> getModules(@NotNull String group);

  /**
   * Get a module by the given name.
   *
   * @param name the name of the module to get.
   * @return the module associated with the name or {@code null} if no such module is loaded.
   */
  @Nullable IModuleWrapper getModule(@NotNull String name);

  /**
   * Loads a module from the given {@code url}.
   *
   * @param url the url to load the module from.
   * @return the loaded module or {@code null} if checks failed or a module from this url is already loaded.
   * @throws ModuleConfigurationNotFoundException         if the file associated with the url doesn't contain a
   *                                                      module.json.
   * @throws ModuleConfigurationPropertyNotFoundException if a required property is missing in the module.json file.
   * @throws com.google.common.base.VerifyException       if required properties are missing in dependency or repository
   *                                                      information.
   * @throws AssertionError                               if any exception occurs during the load of the module.
   */
  @Nullable IModuleWrapper loadModule(@NotNull URL url);

  /**
   * Loads the module by the file provided by the given {@code path}.
   *
   * @param path the path to load the module from.
   * @return the loaded module or {@code null} if checks failed or a module from this path is already loaded.
   * @throws ModuleConfigurationNotFoundException         if the file associated with the url doesn't contain a
   *                                                      module.json.
   * @throws ModuleConfigurationPropertyNotFoundException if a required property is missing in the module.json file.
   * @throws com.google.common.base.VerifyException       if required properties are missing in dependency or repository
   *                                                      information.
   * @throws AssertionError                               if any exception occurs during the load of the module.
   * @see #loadModule(URL)
   */
  @Nullable IModuleWrapper loadModule(@NotNull Path path);

  // TODO: docs
  @NotNull IModuleProvider loadAll();

  /**
   * Starts all modules which are loaded by this provided and can change to the started state.
   *
   * @return the same instance of the class, for chaining.
   * @see IModuleWrapper#getModuleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NotNull IModuleProvider startAll();

  /**
   * Reloads all modules which are loaded by this provided and can change to the started state.
   *
   * @return the same instance of the class, for chaining.
   * @see IModuleWrapper#getModuleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NotNull IModuleProvider reloadAll();

  /**
   * Stops all modules which are loaded by this provided and can change to the stopped state.
   *
   * @return the same instance of the class, for chaining.
   * @see IModuleWrapper#getModuleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NotNull IModuleProvider stopAll();

  /**
   * Unloads all modules which are loaded by this provided and can change to the unloaded state.
   *
   * @return the same instance of the class, for chaining.
   * @see IModuleWrapper#getModuleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NotNull IModuleProvider unloadAll();

  /**
   * Called by an {@link IModuleWrapper} when the module is about to change its lifecycle state.
   *
   * @param wrapper   the wrapper which is changing the lifecycle.
   * @param lifeCycle the lifecycle the wrapper want's to change to.
   * @return If the wrapper is allowed to change the lifecycle to the provided lifecycle.
   */
  boolean notifyPreModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle);

  /**
   * Called by an {@link IModuleWrapper} when the module changed its lifecycle state.
   *
   * @param wrapper   the wrapper which changed the lifecycle.
   * @param lifeCycle the lifecycle the wrapper changed to.
   */
  void notifyPostModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle);
}
