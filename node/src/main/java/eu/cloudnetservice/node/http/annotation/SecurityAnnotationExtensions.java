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

package eu.cloudnetservice.node.http.annotation;

import eu.cloudnetservice.driver.network.http.HttpContextPreprocessor;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.driver.network.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import java.lang.reflect.Method;
import lombok.NonNull;

public final class SecurityAnnotationExtensions {

  private SecurityAnnotationExtensions() {
    throw new UnsupportedOperationException();
  }

  public static void install(@NonNull HttpAnnotationParser<?> annotationParser, @NonNull V2HttpAuthentication auth) {
    annotationParser
      .registerAnnotationProcessor(new BasicAuthProcessor(auth))
      .registerAnnotationProcessor(new BearerAuthProcessor(auth));
  }

  private record BasicAuthProcessor(@NonNull V2HttpAuthentication authentication) implements HttpAnnotationProcessor {

    @Override
    public @NonNull HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        BasicAuth.class,
        (param, annotation) -> (path, context) -> {
          // try to authenticate the user; throw an exception if that failed and the parameter is required
          var authResult = this.authentication.handleBasicLoginRequest(context.request());
          if (!param.isAnnotationPresent(Optional.class) && authResult.failed()) {
            throw new AnnotationHttpHandleException(path, "Unable to authenticate user: " + authResult.errorMessage());
          }

          // put the user into the context
          return authResult.result();
        });
      return (path, ctx) -> ctx.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
    }
  }

  private record BearerAuthProcessor(@NonNull V2HttpAuthentication authentication) implements HttpAnnotationProcessor {

    @Override
    public @NonNull HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        BearerAuth.class,
        (param, annotation) -> (path, context) -> {
          // try to authenticate the user; throw an exception if that failed and the parameter is required
          var authResult = this.authentication.handleBearerLoginRequest(context.request());
          if (!param.isAnnotationPresent(Optional.class) && authResult.failed()) {
            throw new AnnotationHttpHandleException(path, "Unable to authenticate user: " + authResult.errorMessage());
          }

          // put the session into the context
          return authResult.result();
        });
      return (path, ctx) -> ctx.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
    }
  }
}
