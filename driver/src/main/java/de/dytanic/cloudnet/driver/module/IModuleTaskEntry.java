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
import org.jetbrains.annotations.NotNull;

/**
 * A task which will be dynamically created in the runtime for every method in a module's main class which is annotated
 * with {@link ModuleTask}. This annotation will hold all information about the task, and it's invocation lifecycle.
 */
public interface IModuleTaskEntry {

  /**
   * The associated module wrapper this task was detected by.
   *
   * @return the associated module wrapper to this task.
   * @see DefaultModuleWrapper#resolveModuleTasks(IModule)
   */
  @NotNull IModuleWrapper moduleWrapper();

  /**
   * The module (or module main class) this task was detected in.
   *
   * @return the associated module to this task.
   */
  @NotNull IModule module();

  /**
   * The annotation holding the information about this task.
   *
   * @return the task information.
   */
  @NotNull ModuleTask taskInfo();

  /**
   * The method handle this task will invoke when calling {@link #fire()}.
   *
   * @return the annotated detected method.
   */
  @NotNull MethodHandle method();

  /**
   * Get the full method signature of the detected method. This must not be in the standard java signature declaration
   * format. However, this descriptor must be unique for debug reasons.
   *
   * @return a unique descriptor of the method in the main module class.
   */
  @NotNull String fullMethodSignature();

  /**
   * Fires this module task.
   *
   * @throws Throwable anything thrown by the underlying method.
   */
  void fire() throws Throwable;
}
