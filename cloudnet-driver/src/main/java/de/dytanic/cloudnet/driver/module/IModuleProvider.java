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

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

// TODO: add documentation to this
public interface IModuleProvider {

  @Deprecated
  default File getModuleDirectory() {
    return this.getModuleDirectoryPath().toFile();
  }

  @Deprecated
  default void setModuleDirectory(File moduleDirectory) {
    this.setModuleDirectoryPath(moduleDirectory.toPath());
  }

  @NotNull Path getModuleDirectoryPath();

  void setModuleDirectoryPath(@NotNull Path moduleDirectory);

  @Nullable IModuleProviderHandler getModuleProviderHandler();

  void setModuleProviderHandler(@Nullable IModuleProviderHandler moduleProviderHandler);

  @NotNull IModuleDependencyLoader getModuleDependencyLoader();

  void setModuleDependencyLoader(@NotNull IModuleDependencyLoader moduleDependencyLoader);

  @NotNull
  @Unmodifiable Collection<IModuleWrapper> getModules();

  @NotNull
  @Unmodifiable Collection<IModuleWrapper> getModules(@NotNull String group);

  @Nullable IModuleWrapper getModule(@NotNull String name);

  @Nullable IModuleWrapper loadModule(@NotNull URL url);

  @Deprecated
  @ScheduledForRemoval
  default IModuleWrapper loadModule(@NotNull File file) {
    return this.loadModule(file.toPath());
  }

  @Nullable IModuleWrapper loadModule(@NotNull Path path);

  @Deprecated
  @ScheduledForRemoval
  default IModuleProvider loadModule(URL... urls) {
    for (URL url : urls) {
      this.loadModule(url);
    }
    return this;
  }

  @Deprecated
  @ScheduledForRemoval
  default IModuleProvider loadModule(File... files) {
    for (File file : files) {
      this.loadModule(file);
    }
    return this;
  }

  @Deprecated
  @ScheduledForRemoval
  default IModuleProvider loadModule(Path... paths) {
    for (Path path : paths) {
      this.loadModule(path);
    }
    return this;
  }

  @NotNull IModuleProvider startAll();

  @NotNull IModuleProvider stopAll();

  @NotNull IModuleProvider unloadAll();

  boolean notifyPreModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle);
  u
  void notifyPostModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle);
}
