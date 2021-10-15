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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a task a module can define in its main class. This annotation replaces the wideline known (mostly from
 * bukkit or bungeecord) {@code onLoad}, {@code onEnable} and {@code onDisable} methods. Please note that module
 * dependencies are only loaded in the {@link ModuleLifeCycle#STARTED} state and module task targeting the {@link
 * ModuleLifeCycle#UNUSEABLE} will never fire. For more details see the javadoc comments on the enum constants in {@link
 * ModuleLifeCycle}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleTask {

  /**
   * Get the order of this task to be fired within the other task in the same lifecycle. If two tasks with the same
   * lifecycle target and order are defined, the sort operation will fall back to the java natural order which may
   * change from execution to execution.
   * <p>This property defaults to {@code 32}.</p>
   *
   * @return the order in which this task will be fired.
   */
  byte order() default 32;

  /**
   * Get the module lifecycle in which this task should be fired. Please note that tasks targeting the lifecycle {@link
   * ModuleLifeCycle#UNUSEABLE} will be registered but never fired.
   * <p>This property defaults to {@link ModuleLifeCycle#STARTED}</p>
   *
   * @return the module lifecycle in which this task should be fired.
   */
  ModuleLifeCycle event() default ModuleLifeCycle.STARTED;
}
