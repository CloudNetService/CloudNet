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

package eu.cloudnetservice.node.http.annotation;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpContextPreprocessor;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.driver.network.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.RestUser;
import eu.cloudnetservice.node.http.RestUserManagement;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class SecurityAnnotationExtension {

  public void install(@NonNull HttpAnnotationParser<?> annotationParser, @NonNull V2HttpAuthentication auth) {
    annotationParser
      .registerAnnotationProcessor(new BasicAuthProcessor(auth))
      .registerAnnotationProcessor(new BearerAuthProcessor(auth));
  }

  private @Nullable <T> HttpContext handleAuthResult(
    @NonNull HttpContext context,
    @NonNull V2HttpAuthentication.LoginResult<T> result,
    @NonNull Function<T, RestUser> userExtractor,
    @Nullable HandlerScope scope,
    @NonNull String path
  ) {
    // if the auth succeeded check if the user has the required scope
    if (result.succeeded()) {
      var user = userExtractor.apply(result.result());

      // make sure that the requested scope is following the desired scope pattern
      this.ensureScopePattern(scope, path);

      if (!this.validateScope(user, scope)) {
        // the user has no scope for the handler
        context
          .cancelNext(true)
          .closeAfter(true)
          .response()
          .status(HttpResponseCode.UNAUTHORIZED)
          .body(this.buildErrorResponse("Required scope is not set"));
        return null;
      }

      // all fine
      return context;
    }

    // auth failed - set that in the context and drop the request
    context
      .cancelNext(true)
      .closeAfter(true)
      .response()
      .status(HttpResponseCode.UNAUTHORIZED)
      .body(this.buildErrorResponse(result.errorMessage()));
    return null;
  }

  private boolean validateScope(@NonNull RestUser user, @Nullable HandlerScope scope) {
    return scope == null || user.hasOneScopeOf(scope.value());
  }

  private @Nullable HandlerScope resolveScopeAnnotation(@NonNull Method method) {
    var permission = method.getAnnotation(HandlerScope.class);
    return permission == null ? method.getDeclaringClass().getAnnotation(HandlerScope.class) : permission;
  }

  private void ensureScopePattern(@Nullable HandlerScope handlerScope, @NonNull String path) {
    if (handlerScope == null) {
      return;
    }

    for (var scope : handlerScope.value()) {
      if (!RestUserManagement.SCOPE_PATTERN.matcher(scope).matches()) {
        throw new AnnotationHttpHandleException(
          path,
          String.format(
            "Required scope %s does not match the scope pattern %s",
            scope,
            RestUserManagement.SCOPE_PATTERN.pattern()));
      }
    }
  }

  private byte[] buildErrorResponse(@Nullable String reason) {
    return Document.newJsonDocument()
      .append("success", false)
      .append("reason", Objects.requireNonNullElse(reason, "undefined"))
      .toString()
      .getBytes(StandardCharsets.UTF_8);
  }

  private final class BasicAuthProcessor implements HttpAnnotationProcessor {

    private final @NonNull V2HttpAuthentication authentication;

    private BasicAuthProcessor(@NonNull V2HttpAuthentication authentication) {
      this.authentication = authentication;
    }

    @Override
    public @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var scope = SecurityAnnotationExtension.this.resolveScopeAnnotation(method);
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        BasicAuth.class,
        (param, annotation) -> (path, context) -> {
          // try to authenticate the user; throw an exception if that failed and the parameter is required
          var authResult = this.authentication.handleBasicLoginRequest(context.request());
          if (!param.isAnnotationPresent(Optional.class) && authResult.failed()) {
            throw new AnnotationHttpHandleException(
              path,
              "Unable to authenticate user: " + authResult.errorMessage(),
              HttpResponseCode.UNAUTHORIZED,
              SecurityAnnotationExtension.this.buildErrorResponse("Unable to authenticate user"));
          }

          // make sure that the requested scope is following the desired scope pattern
          SecurityAnnotationExtension.this.ensureScopePattern(scope, path);

          // put the user into the context if he has the required scope
          if (SecurityAnnotationExtension.this.validateScope(authResult.result(), scope)) {
            return authResult.result();
          }

          throw new AnnotationHttpHandleException(
            path,
            "User has not the required scope to send a request",
            HttpResponseCode.FORBIDDEN,
            SecurityAnnotationExtension.this.buildErrorResponse("Missing required scope"));
        });
      // check if we got any hints
      if (!hints.isEmpty()) {
        return (path, ctx) -> ctx.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
      }

      // check if the annotation is present on method level
      if (method.isAnnotationPresent(BasicAuth.class)) {
        return (path, ctx) -> {
          // drop the request if the authentication failed
          var authResult = this.authentication.handleBasicLoginRequest(ctx.request());
          return SecurityAnnotationExtension.this.handleAuthResult(ctx, authResult, Function.identity(), scope, path);
        };
      }

      // ok, nothing to do
      return null;
    }
  }

  private final class BearerAuthProcessor implements HttpAnnotationProcessor {

    private final @NonNull V2HttpAuthentication authentication;

    private BearerAuthProcessor(@NonNull V2HttpAuthentication authentication) {
      this.authentication = authentication;
    }

    @Override
    public @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var scope = SecurityAnnotationExtension.this.resolveScopeAnnotation(method);
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        BearerAuth.class,
        (param, annotation) -> (path, context) -> {
          // try to authenticate the user; throw an exception if that failed and the parameter is required
          var authResult = this.authentication.handleBearerLoginRequest(context.request());
          if (!param.isAnnotationPresent(Optional.class) && authResult.failed()) {
            throw new AnnotationHttpHandleException(
              path,
              "Unable to authenticate user: " + authResult.errorMessage(),
              HttpResponseCode.UNAUTHORIZED,
              SecurityAnnotationExtension.this.buildErrorResponse("Unable to authenticate user"));
          }

          // make sure that the requested scope is following the desired scope pattern
          SecurityAnnotationExtension.this.ensureScopePattern(scope, path);

          // put the user into the context if he has the required scope
          if (SecurityAnnotationExtension.this.validateScope(authResult.result().user(), scope)) {
            return authResult.result();
          }

          throw new AnnotationHttpHandleException(
            path,
            "User has not the required scope to send a request",
            HttpResponseCode.FORBIDDEN,
            SecurityAnnotationExtension.this.buildErrorResponse("Missing required scope"));
        });
      // check if we got any hints
      if (!hints.isEmpty()) {
        return (path, ctx) -> ctx.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
      }

      // check if the annotation is present on method level
      if (method.isAnnotationPresent(BearerAuth.class)) {
        return (path, ctx) -> {
          // drop the request if the authentication failed
          var authResult = this.authentication.handleBearerLoginRequest(ctx.request());
          return SecurityAnnotationExtension.this.handleAuthResult(ctx, authResult, HttpSession::user, scope, path);
        };
      }

      // ok, nothing to do
      return null;
    }
  }
}
