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

package eu.cloudnetservice.node.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * This annotation allows users to skip the required confirmation of a command. When this annotation is applied to a
 * method a flag is implicitly appended to the command which can be used to skip the confirmation.
 * <p>
 * The annotations value is used as the flag name, which defaults to {@code confirm}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableConfirmSkipFlag {

  /**
   * The value of this annotation is used as the flag name to skip the confirmation. Should not include the leading
   * dashes.
   * <p>
   * Defaults to {@code confirm}, allowing users to skip the confirmation by appending {@code --confirm}.
   *
   * @return the flag name that allows skipping.
   */
  @NonNull String value() default "confirm";
}
