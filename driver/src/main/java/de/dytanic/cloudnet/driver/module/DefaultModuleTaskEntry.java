/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
import lombok.NonNull;

public class DefaultModuleTaskEntry implements ModuleTaskEntry {

  /**
   * A simple format which allows unique method identifiers. Format: {@code <declaring class>@<method name>()}
   */
  protected static final String METHOD_SIGNATURE_FORMAT = "%s@%s()";

  protected final MethodHandle method;
  protected final ModuleTask moduleTask;
  protected final ModuleWrapper moduleWrapper;
  protected final String fullMethodSignatureCached;

  /**
   * Constructs a new instance of this class.
   *
   * @param wrapper the module wrapper associated with this task entry.
   * @param task    the module task annotation based on which this entry was created.
   * @param method  the method which was annotated with {@link ModuleTask}.
   * @throws IllegalAccessException if access checking for the provided method fails.
   */
  public DefaultModuleTaskEntry(ModuleWrapper wrapper, ModuleTask task, Method method) throws IllegalAccessException {
    this.moduleTask = task;
    this.moduleWrapper = wrapper;
    this.method = MethodHandles.lookup().unreflect(method);
    // init the method signature here, later when the jlrMethod field is gone there is no way to get this information.
    this.fullMethodSignatureCached = String.format(
      METHOD_SIGNATURE_FORMAT, method.getDeclaringClass().getCanonicalName(), method.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Module module() {
    return this.moduleWrapper.module();
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
  public @NonNull ModuleTask taskInfo() {
    return this.moduleTask;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull MethodHandle method() {
    return this.method;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String fullMethodSignature() {
    return this.fullMethodSignatureCached;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fire() throws Throwable {
    this.method.invoke(this.moduleWrapper().module());
  }
}
