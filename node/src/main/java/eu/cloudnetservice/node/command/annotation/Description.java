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

package eu.cloudnetservice.node.command.annotation;

import eu.cloudnetservice.driver.command.CommandInfo;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation specifies the description of a command. The command description is collected into the
 * {@link CommandInfo}. By default, the annotation should contain a translation key, that is resolved in the runtime. If
 * this is not the case the {@link #translatable()} option has to be false.
 *
 * @see CommandInfo
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {

  /**
   * Gets the description for all commands of a class which is annotated with this annotation.
   *
   * @return the description.
   */
  String value();

  /**
   * Gets if the provided description is a translation key, that needs to be resolved at runtime.
   *
   * @return if the provided description is a translation key, that needs to be resolved at runtime.
   */
  boolean translatable() default true;
}
