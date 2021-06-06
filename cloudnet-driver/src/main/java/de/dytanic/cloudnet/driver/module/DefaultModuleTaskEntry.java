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

import java.lang.reflect.Method;

public final class DefaultModuleTaskEntry implements IModuleTaskEntry {

  private final IModuleWrapper moduleWrapper;

  private final ModuleTask taskInfo;

  private final Method handler;

  public DefaultModuleTaskEntry(IModuleWrapper moduleWrapper, ModuleTask taskInfo, Method handler) {
    this.moduleWrapper = moduleWrapper;
    this.taskInfo = taskInfo;
    this.handler = handler;
  }

  @Override
  public IModule getModule() {
    return this.moduleWrapper.getModule();
  }

  public IModuleWrapper getModuleWrapper() {
    return this.moduleWrapper;
  }

  public ModuleTask getTaskInfo() {
    return this.taskInfo;
  }

  public Method getHandler() {
    return this.handler;
  }
}
