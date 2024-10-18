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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import lombok.NonNull;

/**
 * Represents the default implementation of the module.
 *
 * @see DriverModule
 * @since 4.0
 */
public class DefaultModule implements Module {

  protected ClassLoader classLoader;
  protected ModuleWrapper moduleWrapper;
  protected ModuleConfiguration moduleConfig;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(@NonNull ClassLoader loader, @NonNull ModuleWrapper wrapper, @NonNull ModuleConfiguration config) {
    // ensure that this is not initialized
    Preconditions.checkArgument(this.classLoader == null || this.moduleWrapper == null || this.moduleConfig == null,
      "Cannot call init twice on a module wrapper");

    this.classLoader = loader;
    this.moduleWrapper = wrapper;
    this.moduleConfig = config;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper moduleWrapper() {
    return this.moduleWrapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassLoader classLoader() {
    return this.classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleConfiguration moduleConfig() {
    return this.moduleConfig;
  }
}
