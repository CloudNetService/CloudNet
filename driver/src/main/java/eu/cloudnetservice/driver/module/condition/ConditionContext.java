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

package eu.cloudnetservice.driver.module.condition;

import eu.cloudnetservice.driver.module.ModuleManager;
import eu.cloudnetservice.driver.module.metadata.ModuleMetadata;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;

/**
 * Used to provide contextual information to condition processors during class introspection.
 *
 * @since 4.0
 */
public interface ConditionContext {

  /**
   * Get the descriptor of the class for which the condition checks are currently executed.
   *
   * @return the descriptor of the class for which the condition checks are currently executed.
   */
  @NonNull
  ClassDesc targetClass();

  /**
   * Get the name of the method for which the condition checking is currently being executed.
   *
   * @return the name of the method for which the condition checking is currently being executed.
   */
  @NonNull
  String targetMethodName();

  /**
   * Get the descriptor of the method for which the condition checking is currently being executed.
   *
   * @return the descriptor of the method for which the condition checking is currently being executed.
   */
  @NonNull
  MethodTypeDesc targetMethodDescriptor();

  /**
   * Get the module manager that is currently executing the module loading process.
   *
   * @return the module manager.
   */
  @NonNull
  ModuleManager moduleManager();

  /**
   * Get the metadata of the module that is currently being loaded and which holds the current target class.
   *
   * @return the metadata of the module that is currently being loaded.
   */
  @NonNull
  ModuleMetadata moduleMetadata();

  /**
   * Get the class loader that is used for the module that is currently being loaded.
   *
   * @return the class loader of the module being loaded.
   */
  @NonNull
  ClassLoader moduleClassLoader();
}
