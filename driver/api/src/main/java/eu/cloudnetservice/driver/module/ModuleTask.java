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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a method that a module can specify. A method annotated with this annotation is captured during the runtime
 * in the module task entry. These methods are like startup and stop methods and are invoked when the module switches to
 * the specified module lifecycle.
 * <p>
 * The dependencies of a module are loaded for the first time in the {@link ModuleLifeCycle#STARTED} lifecycle. All
 * module tasks that target the {@link ModuleLifeCycle#UNUSABLE} lifecycle are never invoked.
 *
 * @see Module
 * @see ModuleTaskEntry
 * @see ModuleLifeCycle
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleTask {

  /**
   * Get the order of this task to be fired within the other task in the same lifecycle. If two tasks with the same
   * lifecycle target and order are defined, the sort operation will fall back to the java natural order which may
   * change from execution to execution.
   * <p>This property defaults to 32.</p>
   *
   * @return the order in which this task will be fired.
   */
  byte order() default 32;

  /**
   * Get the module lifecycle in which this task should be fired. Please note that tasks targeting the lifecycle {@link
   * ModuleLifeCycle#UNUSABLE} will be registered but never fired.
   * <p>This property defaults to {@link ModuleLifeCycle#STARTED}</p>
   *
   * @return the module lifecycle in which this task should be fired.
   */
  ModuleLifeCycle lifecycle() default ModuleLifeCycle.STARTED;
}
