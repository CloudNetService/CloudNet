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

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

@Deprecated
@ScheduledForRemoval
public class ModuleProviderHandlerAdapter implements IModuleProviderHandler {

  @Override
  public boolean handlePreModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
    return true;
  }

  @Override
  public void handlePostModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
  }

  @Override
  public boolean handlePreModuleStart(@NotNull IModuleWrapper moduleWrapper) {
    return true;
  }

  @Override
  public void handlePostModuleStart(@NotNull IModuleWrapper moduleWrapper) {
  }

  @Override
  public boolean handlePreModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    return true;
  }

  @Override
  public void handlePostModuleStop(@NotNull IModuleWrapper moduleWrapper) {
  }

  @Override
  public void handlePreModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
  }

  @Override
  public void handlePostModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
  }

  @Override
  public void handlePreInstallDependency(@NotNull ModuleConfiguration configuration, @NotNull ModuleDependency dependency) {
  }

  @Override
  public void handlePostInstallDependency(@NotNull ModuleConfiguration configuration, @NotNull ModuleDependency dependency) {
  }
}
