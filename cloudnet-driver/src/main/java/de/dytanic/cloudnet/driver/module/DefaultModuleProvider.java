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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultModuleProvider implements IModuleProvider {

  protected static final Path DEFAULT_MODULE_DIR = Paths.get("modules");
  protected static final Logger LOGGER = LogManager.getLogger(DefaultModuleProvider.class);
  protected static final IModuleDependencyLoader DEFAULT_DEPENDENCY_LOADER = new DefaultMemoryModuleDependencyLoader();

  protected final Collection<DefaultModuleWrapper> modules = new CopyOnWriteArrayList<>();

  protected Path moduleDirectory;
  protected IModuleProviderHandler moduleProviderHandler;
  protected IModuleDependencyLoader moduleDependencyLoader;

  public DefaultModuleProvider() {
    this(DEFAULT_MODULE_DIR, DEFAULT_DEPENDENCY_LOADER);
  }

  public DefaultModuleProvider(Path moduleDirectory, IModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDirectory = moduleDirectory;
    this.moduleDependencyLoader = moduleDependencyLoader;
  }

  @Override
  public @NotNull Path getModuleDirectoryPath() {
    return this.moduleDirectory;
  }

  @Override
  public void setModuleDirectoryPath(@NotNull Path moduleDirectory) {
    this.moduleDirectory = Preconditions.checkNotNull(moduleDirectory, "moduleDirectory");
  }

  @Override
  public @Nullable IModuleProviderHandler getModuleProviderHandler() {
    return this.moduleProviderHandler;
  }

  @Override
  public void setModuleProviderHandler(@Nullable IModuleProviderHandler moduleProviderHandler) {
    this.moduleProviderHandler = moduleProviderHandler;
  }

  @Override
  public @NotNull IModuleDependencyLoader getModuleDependencyLoader() {
    return this.moduleDependencyLoader;
  }

  @Override
  public void setModuleDependencyLoader(@NotNull IModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDependencyLoader = Preconditions.checkNotNull(moduleDependencyLoader, "moduleDependencyLoader");
  }

  @Override
  public @NotNull Collection<IModuleWrapper> getModules() {
    return Collections.unmodifiableCollection(this.modules);
  }

  @Override
  public @NotNull Collection<IModuleWrapper> getModules(@NotNull String group) {
    return this.modules.stream()
      .filter(module -> module.getModuleConfiguration().getGroup().equals(group))
      .collect(Collectors.toList());
  }

  @Override
  public IModuleWrapper getModule(@NotNull String name) {
    return this.modules.stream()
      .filter(module -> module.getModuleConfiguration().getName().equals(name))
      .findFirst().orElse(null);
  }

  @Override
  public IModuleWrapper loadModule(@NotNull URL url) {
    return null; // TODO
  }

  @Override
  public IModuleWrapper loadModule(@NotNull Path path) {
    try {
      return this.loadModule(Preconditions.checkNotNull(path, "path").toUri().toURL());
    } catch (MalformedURLException exception) {
      LOGGER.severe("Unable to resolve url of module path", exception);
      return null;
    }
  }

  @Override
  public @NotNull IModuleProvider startAll() {
    for (DefaultModuleWrapper module : this.modules) {
      module.startModule();
    }
    return this;
  }

  @Override
  public @NotNull IModuleProvider stopAll() {
    for (DefaultModuleWrapper module : this.modules) {
      module.stopModule();
    }
    return this;
  }

  @Override
  public @NotNull IModuleProvider unloadAll() {
    for (DefaultModuleWrapper module : this.modules) {
      module.unloadModule();
    }
    return this;
  }

  @Override
  public boolean notifyPreModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle) {
    // todo: handle here (or in post) when the module gets unloaded (f. Ex. remove it from the known modules)
    // post the change to the handler (if one is set)
    IModuleProviderHandler handler = this.moduleProviderHandler;
    if (handler != null) {
      switch (lifeCycle) {
        case LOADED:
          return handler.handlePreModuleLoad(wrapper);
        case STARTED:
          return handler.handlePreModuleStart(wrapper);
        case STOPPED:
          return handler.handlePreModuleStop(wrapper);
        case UNLOADED:
          handler.handlePreModuleUnload(wrapper);
          break;
        default:
          break;
      }
    }
    // if there is no handler or the handler can't change the result - just do it
    return true;
  }

  @Override
  public void notifyPostModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle) {
    // post the change to the handler (if one is set)
    IModuleProviderHandler handler = this.moduleProviderHandler;
    if (handler != null) {
      switch (lifeCycle) {
        case LOADED:
          handler.handlePostModuleLoad(wrapper);
          break;
        case STARTED:
          handler.handlePostModuleStart(wrapper);
          break;
        case STOPPED:
          handler.handlePostModuleStop(wrapper);
          break;
        case UNLOADED:
          handler.handlePostModuleUnload(wrapper);
          break;
        default:
          break;
      }
    }
  }
}
