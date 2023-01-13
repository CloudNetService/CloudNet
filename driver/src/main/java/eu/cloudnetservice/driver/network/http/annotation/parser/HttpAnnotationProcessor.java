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

package eu.cloudnetservice.driver.network.http.annotation.parser;

import eu.cloudnetservice.driver.network.http.HttpContextPreprocessor;
import java.lang.reflect.Method;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A processor for an annotation which can be added to a http handler parameter.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface HttpAnnotationProcessor {

  /**
   * Builds a preprocessor for the annotation which is supported by this processor. The preprocessor will be called
   * before the actual http handler is called, allowing the parser to modify or even abort the request.
   * <p>
   * All processors will be called for a method, unless this processor overrides the {@code shouldProcess} method and
   * decides whether to handle the method or not.
   * <p>
   * If this processor returns null it indicates that it has no interest in adding a preprocessor.
   *
   * @param method          the method which gets processed currently.
   * @param handlerInstance the instance of the handler class in which the method is located.
   * @return a preprocessor for a http request context, or null if there is no need for a preprocessor.
   * @throws NullPointerException if the given method or handler instance is null.
   */
  @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handlerInstance);

  /**
   * Checks if this processor should process the given method. This defaults to true.
   *
   * @param method          the method which gets processed currently.
   * @param handlerInstance the instance of the handler class in which the method is located.
   * @return true if this processor wants to provide a preprocessor, false otherwise.
   * @throws NullPointerException if the given method or handler instance is null.
   */
  default boolean shouldProcess(@NonNull Method method, @NonNull Object handlerInstance) {
    return true;
  }
}
