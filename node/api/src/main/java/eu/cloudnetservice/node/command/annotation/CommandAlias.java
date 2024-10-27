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
 * This annotation specifies all aliases of a command. The processing of this annotation is done in the runtime to
 * collect all aliases of a command for the {@link CommandInfo}
 *
 * @see CommandInfo
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandAlias {

  /**
   * The String array should contain all aliases for the commands that are located in the annotated class.
   * <p>
   * Note: The aliases should not contain the root name of the command.
   *
   * @return all aliases for the commands of a class.
   */
  String[] value();
}
