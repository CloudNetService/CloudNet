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

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The module provider allows access to all loaded modules and the module paths. It keeps track of all known modules and
 * provides access their module wrapper and changing the module lifecycle for the modules.
 *
 * @see ModuleWrapper
 * @see ModuleLifeCycle
 * @see ModuleProviderHandler
 * @since 4.0
 */
public interface ModuleProvider {

  /**
   * Get the base directory of this module provider. It will be used to provide the data directory for modules which did
   * not specifically set a data directory.
   *
   * @return the base directory of this module provider.
   * @see #moduleDirectoryPath(Path)
   */
  @NonNull
  Path moduleDirectoryPath();

  /**
   * Sets the base directory of this module provider. It will be used to provide the data directory for modules which
   * did not specifically set a data directory.
   *
   * @param moduleDirectory the module directory to use.
   * @throws NullPointerException if moduleDirectory is null.
   * @see #moduleDirectoryPath()
   */
  void moduleDirectoryPath(@NonNull Path moduleDirectory);

  /**
   * Get the module provider handler of this provider or null when no handler is specified.
   *
   * @return the module provider handler of this provider or null.
   * @see #moduleProviderHandler(ModuleProviderHandler)
   */
  @Nullable ModuleProviderHandler moduleProviderHandler();

  /**
   * Sets the module provider handler of this provider.
   *
   * @param moduleProviderHandler the new module provider to use or null when no handler should be used.
   * @throws NullPointerException if the given module provider handler is null.
   * @see #moduleProviderHandler()
   */
  void moduleProviderHandler(@Nullable ModuleProviderHandler moduleProviderHandler);

  /**
   * Get the module dependency loader. It's used to load all dependencies of all modules.
   *
   * @return the module provider handler used for this provider.
   * @see #moduleDependencyLoader(ModuleDependencyLoader)
   */
  @NonNull
  ModuleDependencyLoader moduleDependencyLoader();

  /**
   * Sets the module dependency loader which should be used by this provider.
   *
   * @param moduleDependencyLoader the module dependency loader to use.
   * @throws NullPointerException if moduleDependencyLoader is null.
   * @see #moduleDependencyLoader()
   */
  void moduleDependencyLoader(@NonNull ModuleDependencyLoader moduleDependencyLoader);

  /**
   * Get all loaded, started, stopped modules provided by this provider.
   *
   * @return an immutable set of all loaded, started, stopped modules provided by this provider.
   * @see ModuleLifeCycle
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleWrapper> modules();

  /**
   * Get all loaded, started, stopped modules provided by this provider which have the specific given group.
   *
   * @param group the group id of the modules to get.
   * @return an immutable set of all loaded, started, stopped modules provided by this provider which have the specific
   * given group.
   * @throws NullPointerException if group is null.
   * @see ModuleLifeCycle
   */
  @NonNull
  @Unmodifiable
  Collection<ModuleWrapper> modules(@NonNull String group);

  /**
   * Get a module by the given name.
   *
   * @param name the name of the module to get.
   * @return the module associated with the name or null if no such module is loaded.
   * @throws NullPointerException if name is null.
   */
  @Nullable ModuleWrapper module(@NonNull String name);

  /**
   * Loads a module from the given url.
   *
   * @param url the url to load the module from.
   * @return the loaded module or null if checks failed or a module from this url is already loaded.
   * @throws ModuleConfigurationNotFoundException if the file associated with the url doesn't contain a module.json.
   * @throws NullPointerException                 if required properties are missing in dependency or repository
   *                                              information.
   * @throws AssertionError                       if any exception occurs during the load of the module.
   * @throws NullPointerException                 if url is null.
   */
  @Nullable ModuleWrapper loadModule(@NonNull URL url);

  /**
   * Loads the module by the file provided by the given path.
   *
   * @param path the path to load the module from.
   * @return the loaded module or null if checks failed or a module from this path is already loaded.
   * @throws ModuleConfigurationNotFoundException if the file associated with the url doesn't contain a module.json.
   * @throws NullPointerException                 if required properties are missing in dependency or repository
   *                                              information.
   * @throws AssertionError                       if any exception occurs during the load of the module.
   * @throws NullPointerException                 if path is null.
   * @see #loadModule(URL)
   */
  @Nullable ModuleWrapper loadModule(@NonNull Path path);

  /**
   * Loads all modules which files are located at the module directory.
   *
   * @return the same instance of the class, for chaining.
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NonNull
  ModuleProvider loadAll();

  /**
   * Starts all modules which are loaded by this provided and can change to the started state.
   *
   * @return the same instance of the class, for chaining.
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NonNull
  ModuleProvider startAll();

  /**
   * Reloads all modules which are loaded by this provided and can change to the started state.
   *
   * @return the same instance of the class, for chaining.
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NonNull
  ModuleProvider reloadAll();

  /**
   * Stops all modules which are loaded by this provided and can change to the stopped state.
   *
   * @return the same instance of the class, for chaining.
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NonNull
  ModuleProvider stopAll();

  /**
   * Unloads all modules which are loaded by this provided and can change to the unloaded state.
   *
   * @return the same instance of the class, for chaining.
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   */
  @NonNull
  ModuleProvider unloadAll();

  /**
   * Called by an {@link ModuleWrapper} when the module is about to change its lifecycle state.
   *
   * @param wrapper   the wrapper which is changing the lifecycle.
   * @param lifeCycle the lifecycle the wrapper want's to change to.
   * @return If the wrapper is allowed to change the lifecycle to the provided lifecycle.
   * @throws NullPointerException if wrapper or lifeCycle is null.
   */
  boolean notifyPreModuleLifecycleChange(@NonNull ModuleWrapper wrapper, @NonNull ModuleLifeCycle lifeCycle);

  /**
   * Called by an {@link ModuleWrapper} when the module changed its lifecycle state.
   *
   * @param wrapper   the wrapper which changed the lifecycle.
   * @param lifeCycle the lifecycle the wrapper changed to.
   * @throws NullPointerException if wrapper or lifeCycle is null.
   */
  void notifyPostModuleLifecycleChange(@NonNull ModuleWrapper wrapper, @NonNull ModuleLifeCycle lifeCycle);
}
