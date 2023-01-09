/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Get all values of the associated query key. The value passed to the associated parameter is {@code List<String>}.
 * This annotation can be combined with {@code @Optional} to mark the parameter as optional, an empty list is passed to
 * the parameter in that case.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestQueryParam {

  /**
   * Get the key of the query parameters to retrieve.
   *
   * @return the key of the query parameters to retrieve.
   */
  @NonNull String value();

  /**
   * Sets whether null should be injected for the method parameter instead of an empty collection if no query parameter
   * with the specified key is present and the parameter marked as optional.
   *
   * @return if null should be injected instead of an empty collection if no query parameters with the key are present.
   */
  boolean nullWhenAbsent() default false;
}
