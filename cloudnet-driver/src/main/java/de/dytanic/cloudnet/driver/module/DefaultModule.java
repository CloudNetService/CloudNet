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
import org.jetbrains.annotations.NotNull;

public class DefaultModule implements IModule {

  protected ClassLoader classLoader;
  protected IModuleWrapper moduleWrapper;
  protected ModuleConfiguration moduleConfig;

  protected void init(@NotNull ClassLoader loader, @NotNull IModuleWrapper wrapper,
    @NotNull ModuleConfiguration config) {
    // ensure that this is not initialized
    Preconditions.checkArgument(this.classLoader == null || this.moduleWrapper == null || this.moduleConfig == null,
      "Cannot call init twice");

    this.classLoader = Preconditions.checkNotNull(loader, "loader");
    this.moduleWrapper = Preconditions.checkNotNull(wrapper, "wrapper");
    this.moduleConfig = Preconditions.checkNotNull(config, "config");
  }

  public @NotNull IModuleWrapper getModuleWrapper() {
    return this.moduleWrapper;
  }

  public @NotNull ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public @NotNull ModuleConfiguration getModuleConfig() {
    return this.moduleConfig;
  }
}
