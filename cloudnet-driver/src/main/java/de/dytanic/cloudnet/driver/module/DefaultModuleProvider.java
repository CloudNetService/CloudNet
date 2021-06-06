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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class DefaultModuleProvider implements IModuleProvider {

  protected final Collection<DefaultModuleWrapper> moduleWrappers = new CopyOnWriteArrayList<>();

  protected IModuleProviderHandler moduleProviderHandler = new ModuleProviderHandlerAdapter();
  protected IModuleDependencyLoader moduleDependencyLoader = new DefaultMemoryModuleDependencyLoader();

  private Path moduleDirectory = Paths.get("modules");

  @Override
  public Collection<IModuleWrapper> getModules() {
    return Collections.unmodifiableCollection(this.moduleWrappers);
  }

  @Override
  public Collection<IModuleWrapper> getModules(String group) {
    Preconditions.checkNotNull(group);

    return this.getModules().stream()
      .filter(defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().group.equals(group))
      .collect(Collectors.toList());
  }

  @Override
  public IModuleWrapper getModule(String name) {
    Preconditions.checkNotNull(name);

    return this.moduleWrappers.stream()
      .filter(defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().getName().equals(name)).findFirst()
      .orElse(null);
  }

  @Override
  public IModuleWrapper loadModule(URL url) {
    Preconditions.checkNotNull(url);

    DefaultModuleWrapper moduleWrapper = null;
    if (this.moduleWrappers.stream()
      .anyMatch(defaultModuleWrapper -> defaultModuleWrapper.getUrl().toString().equalsIgnoreCase(url.toString()))) {
      return null;
    }

    try {
      this.moduleWrappers.add(moduleWrapper = new DefaultModuleWrapper(this, url, this.moduleDirectory));
      moduleWrapper.loadModule();
    } catch (Throwable throwable) {
      throwable.printStackTrace();

      if (moduleWrapper != null) {
        moduleWrapper.unloadModule();
      }
    }

    return moduleWrapper;
  }

  @Override
  public IModuleWrapper loadModule(File file) {
    Preconditions.checkNotNull(file);

    return this.loadModule(file.toPath());
  }

  @Override
  public IModuleWrapper loadModule(Path path) {
    Preconditions.checkNotNull(path);

    try {
      return this.loadModule(path.toUri().toURL());
    } catch (MalformedURLException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  @Override
  public IModuleProvider loadModule(URL... urls) {
    Preconditions.checkNotNull(urls);

    for (URL url : urls) {
      this.loadModule(url);
    }

    return this;
  }

  @Override
  public IModuleProvider loadModule(File... files) {
    Preconditions.checkNotNull(files);

    for (File file : files) {
      this.loadModule(file);
    }

    return this;
  }

  @Override
  public IModuleProvider loadModule(Path... paths) {
    Preconditions.checkNotNull(paths);

    for (Path path : paths) {
      this.loadModule(path);
    }

    return this;
  }

  @Override
  public IModuleProvider startAll() {
    for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
      moduleWrapper.startModule();
    }

    return this;
  }

  @Override
  public IModuleProvider stopAll() {
    for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
      moduleWrapper.stopModule();
    }

    return this;
  }

  @Override
  public IModuleProvider unloadAll() {
    for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
      moduleWrapper.unloadModule();
    }

    return this;
  }

  @Override
  public Path getModuleDirectoryPath() {
    return this.moduleDirectory;
  }

  @Override
  public void setModuleDirectoryPath(Path moduleDirectory) {
    this.moduleDirectory = Preconditions.checkNotNull(moduleDirectory);
  }

  public IModuleProviderHandler getModuleProviderHandler() {
    return this.moduleProviderHandler;
  }

  public void setModuleProviderHandler(IModuleProviderHandler moduleProviderHandler) {
    this.moduleProviderHandler = moduleProviderHandler;
  }

  public IModuleDependencyLoader getModuleDependencyLoader() {
    return this.moduleDependencyLoader;
  }

  public void setModuleDependencyLoader(IModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDependencyLoader = moduleDependencyLoader;
  }
}
