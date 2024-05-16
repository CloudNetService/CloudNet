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

package eu.cloudnetservice.driver.network.http.annotation;

import eu.cloudnetservice.driver.network.http.annotation.parser.DefaultHttpAnnotationParser;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Get the first value of the associated query key. The value passed to the associated parameter is {@code String}. This
 * annotation can be combined with {@code @Optional} to mark the parameter as optional.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FirstRequestQueryParam {

  /**
   * Get the key of the first query parameter to retrieve.
   *
   * @return the key of the first query parameter to retrieve.
   */
  @NonNull String value();

  /**
   * Gets the default value to inject if no query parameter with the specified key is given and the parameter marked as
   * optional. This defaults to {@code __NULL__} which will inject null for the parameter.
   *
   * @return the default value to inject if this query parameter is not present.
   */
  @NonNull String def() default DefaultHttpAnnotationParser.DEFAULTS_TO_NULL_MASK;
}
