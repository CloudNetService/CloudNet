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

import lombok.NonNull;

/**
 * This ModuleTaskEntry is dynamically generated in the runtime. The entry represents one method in the main class of a
 * module that is annotated with the module task annotation. This entry contains all necessary information for
 * invocation of the corresponding method.
 *
 * @see ModuleTask
 * @see Module
 * @since 4.0
 */
public interface ModuleTaskEntry {

  /**
   * The associated module wrapper this task was detected by.
   *
   * @return the associated module wrapper to this task.
   */
  @NonNull
  ModuleWrapper moduleWrapper();

  /**
   * The module (or module main class) this task was detected in.
   *
   * @return the associated module to this task.
   */
  @NonNull
  Module module();

  /**
   * The annotation holding the information about this task.
   *
   * @return the task information.
   */
  @NonNull
  ModuleTask taskInfo();

  /**
   * Get the full method signature of the detected method. This must not be in the standard java signature declaration
   * format. However, this descriptor must be unique for debug reasons.
   *
   * @return a unique descriptor of the method in the main module class.
   */
  @NonNull
  String fullMethodSignature();

  /**
   * Fires this module task.
   *
   * @throws Throwable anything thrown by the underlying method.
   */
  void fire() throws Throwable;
}
