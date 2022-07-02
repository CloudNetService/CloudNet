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
import java.lang.reflect.Parameter;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a hint which can be added to a http context while pre-processing to indicate that the parameter at the
 * specified index should get overridden with the value returned by the given resolver.
 *
 * @param index         the index of the parameter to override.
 * @param target        the parameter this hint is associated with.
 * @param valueResolver the resolver of the value to inject into handling http methods depending on the context.
 * @since 4.0
 */
public record ParameterInvocationHint(
  int index,
  @NonNull Parameter target,
  @NonNull BiFunction<String, HttpContext, Object> valueResolver
) {

  /**
   * Resolves the value of this hint which should be injected into the method parameter at the associated index
   * depending on the calling context and path.
   *
   * @param requestPath the path to which the http request to handle by the method was sent.
   * @param context     the http context to resolve the parameter value based on.
   * @return the value to inject into the parameter, can be null.
   * @throws NullPointerException if the given request path or context is null.
   */
  public @Nullable Object resolveValue(@NonNull String requestPath, @NonNull HttpContext context) {
    return this.valueResolver.apply(requestPath, context);
  }
}
