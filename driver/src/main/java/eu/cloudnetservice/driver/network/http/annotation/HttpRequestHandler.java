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

import eu.cloudnetservice.driver.network.http.HttpHandler;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Represents a method which can handle http requests sent to one of the provided paths using and request methods. The
 * first parameter of an annotated method must (and will) be the request HttpContext.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpRequestHandler {

  /**
   * Get the paths to which the request can be sent in order to call the associated handling method.
   *
   * @return the url paths the associated method is handling.
   */
  @NonNull String[] paths();

  /**
   * Get the methods which can be used to call the associated handling method, defaults to GET.
   *
   * @return the http request methods the associated method is handling.
   */
  @NonNull String[] methods() default "GET";

  /**
   * Get the priority of the associated handling method.
   *
   * @return the priority of the handling method.
   */
  int priority() default HttpHandler.PRIORITY_NORMAL;

  /**
   * Get the port to which this handler is listening to exclusively.
   *
   * @return the port this handler is listening to.
   */
  int port() default -1;
}
