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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;

public class DefaultModuleTaskEntry implements IModuleTaskEntry {

  protected static final String METHOD_SIGNATURE_FORMAT = "%s@%s()";

  protected final Method jlrMethod;
  protected final MethodHandle method;
  protected final ModuleTask moduleTask;
  protected final IModuleWrapper moduleWrapper;
  protected final String fullMethodSignatureCached;

  public DefaultModuleTaskEntry(IModuleWrapper wrapper, ModuleTask task, Method method) throws IllegalAccessException {
    this.jlrMethod = method;
    this.moduleTask = task;
    this.moduleWrapper = wrapper;
    this.method = MethodHandles.lookup().unreflect(method);
    // init the method signature here, later when the jlrMethod field is gone there is no way to get this information.
    this.fullMethodSignatureCached = String.format(
      METHOD_SIGNATURE_FORMAT, method.getDeclaringClass().getCanonicalName(), method.getName());
  }

  @Override
  public @NotNull IModule getModule() {
    return this.moduleWrapper.getModule();
  }

  @Override
  public @NotNull IModuleWrapper getModuleWrapper() {
    return this.moduleWrapper;
  }

  @Override
  public @NotNull ModuleTask getTaskInfo() {
    return this.moduleTask;
  }

  @Override
  public @NotNull MethodHandle getMethod() {
    return this.method;
  }

  @Override
  @Deprecated
  public Method getHandler() {
    return this.jlrMethod;
  }

  @Override
  public @NotNull String getFullMethodSignature() {
    return this.fullMethodSignatureCached;
  }

  @Override
  public void fire() throws Throwable {
    this.method.invokeExact(this.getModuleWrapper().getModule());
  }
}
