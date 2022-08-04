/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.network.http.HttpContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A very small utility class to more easily process annotations added to method parameters.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class HttpAnnotationProcessorUtil {

  private HttpAnnotationProcessorUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Applies each parameter of the given method which is annotated with the given annotation to the given mapper to
   * determine a resolver for a value which should get injected into the parameter when receiving a http request. The
   * mapper can return null to indicate that another processor for the same annotation should handle the request. If no
   * resolver was determined for the parameter, each http call to the route associated with the method will result in an
   * exception as no value for the parameter is present.
   *
   * @param method         the http handler method to process.
   * @param annotationType the raw type of the annotation to handle exclusively.
   * @param mapper         the mapper to determine the value resolver for each parameter of the method.
   * @param <A>            the full type of the annotation.
   * @return a collection of invocation hints which can be added to an incoming http request context.
   * @throws NullPointerException if the given method, annotation type or value resolver mapper is null.
   */
  public static @NonNull <A extends Annotation> Collection<ParameterInvocationHint> mapParameters(
    @NonNull Method method,
    @NonNull Class<A> annotationType,
    @NonNull BiFunction<Parameter, A, BiFunction<String, HttpContext, Object>> mapper
  ) {
    var parameters = method.getParameters();
    List<ParameterInvocationHint> hints = new ArrayList<>();

    for (int i = 0; i < parameters.length; i++) {
      var param = parameters[i];
      var annotation = param.getAnnotation(annotationType);

      // check if the annotation is present on the method
      if (annotation != null) {
        // combine the annotation and parameter to a value resolver
        var valueResolver = mapper.apply(param, annotation);
        if (valueResolver != null) {
          hints.add(new ParameterInvocationHint(i, param, valueResolver));
        }
      }
    }

    return hints;
  }
}
