/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Annotation that marks tasks to execute. This annotation can only be added to methods in the module main class, as
 * discovery of other instances using the annotation would be impossible. The annotation can be added to any method in
 * the module main class hierarchy, each method will be invoked. Methods are discovered top-down, so first the actual
 * main class is checked, then any extending class. If a method is overridden, the base method will not be invoked.
 * Placing this annotation on an abstract method has no effect.
 * <p>
 * The invocation order of module tasks is evaluated after all methods were discovered. This means that the invocation
 * order of the methods is not tied to the encounter order at all.
 * <p>
 * Parameters of module task methods will be resolved using dependency injection. Module task methods can also be
 * conditional. Methods that do not meet the required conditions will not be discovered and the module task will never
 * get executed.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleTask {

  /**
   * Priority constant for module tasks that will always be ordered first. This constant is equivalent to {@code 127}.
   */
  byte PRIORITY_FIRST = Byte.MAX_VALUE;
  /**
   * Priority constant for module tasks that should be executed early during the module task execution. This constant is
   * equivalent to {@code 63}.
   */
  byte PRIORITY_EARLY = Byte.MAX_VALUE / 2;
  /**
   * Priority constant for module tasks with medium priority. This constant is equivalent to {@code 0}.
   */
  byte PRIORITY_MEDIUM = 0;
  /**
   * Priority constant for module tasks that should be executed late during the module task execution. This constant is
   * equivalent to {@code -64}.
   */
  byte PRIORITY_LATE = Byte.MIN_VALUE / 2;
  /**
   * Priority constant for module tasks that should run last.  This constant is equivalent to {@code -128}.
   */
  byte PRIORITY_LAST = Byte.MIN_VALUE;

  /**
   * Get the states of the module when this module task should be executed.
   *
   * @return the states of the module when this module task should be executed.
   */
  @NonNull
  ModuleState[] states();

  /**
   * Get the priority of this module task. Defaults to medium priority.
   *
   * @return the priority of this module task.
   */
  byte priority() default PRIORITY_MEDIUM;
}
