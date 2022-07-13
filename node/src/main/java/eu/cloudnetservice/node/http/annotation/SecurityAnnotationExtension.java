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

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpContextPreprocessor;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.driver.network.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class SecurityAnnotationExtension {

  private SecurityAnnotationExtension() {
    throw new UnsupportedOperationException();
  }

  public static void install(@NonNull HttpAnnotationParser<?> annotationParser, @NonNull V2HttpAuthentication auth) {
    annotationParser
      .registerAnnotationProcessor(new BasicAuthProcessor(auth))
      .registerAnnotationProcessor(new BearerAuthProcessor(auth));
  }

  private static @Nullable <T> HttpContext handleAuthResult(
    @NonNull HttpContext context,
    @NonNull V2HttpAuthentication.LoginResult<T> result,
    @NonNull Function<T, PermissionUser> userExtractor,
    @Nullable HandlerPermission permission
  ) {
    // if the auth succeeded check if the user has the required permission
    if (result.succeeded()) {
      var user = userExtractor.apply(result.result());
      if (!validatePermission(user, permission)) {
        // the user has no permission for the handler
        context
          .cancelNext(true)
          .closeAfter(true)
          .response()
          .status(HttpResponseCode.UNAUTHORIZED)
          .body(buildErrorResponse("Required permission is not set"));
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
      .body(buildErrorResponse(result.errorMessage()));
    return null;
  }

  private static boolean validatePermission(@NonNull PermissionUser user, @Nullable HandlerPermission permission) {
    var permissionManagement = CloudNetDriver.instance().permissionManagement();
    return permission == null || permissionManagement.hasPermission(user, Permission.of(permission.value()));
  }

  private static @Nullable HandlerPermission resolvePermissionAnnotation(@NonNull Method method) {
    var permission = method.getAnnotation(HandlerPermission.class);
    return permission == null ? method.getDeclaringClass().getAnnotation(HandlerPermission.class) : permission;
  }

  private static byte[] buildErrorResponse(@Nullable String reason) {
    return JsonDocument.newDocument("success", false)
      .append("reason", Objects.requireNonNullElse(reason, "undefined"))
      .toString()
      .getBytes(StandardCharsets.UTF_8);
  }

  private record BasicAuthProcessor(@NonNull V2HttpAuthentication authentication) implements HttpAnnotationProcessor {

    @Override
    public @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var permission = resolvePermissionAnnotation(method);
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
              buildErrorResponse("Unable to authenticate user"));
          }

          // put the user into the context if he has the required permission
          if (validatePermission(authResult.result(), permission)) {
            return authResult.result();
          }

          throw new AnnotationHttpHandleException(
            path,
            "User has not the required permission to send a request",
            HttpResponseCode.FORBIDDEN,
            buildErrorResponse("Missing required permission"));
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
          return handleAuthResult(ctx, authResult, Function.identity(), permission);
        };
      }

      // ok, nothing to do
      return null;
    }
  }

  private record BearerAuthProcessor(@NonNull V2HttpAuthentication authentication) implements HttpAnnotationProcessor {

    @Override
    public @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var permission = resolvePermissionAnnotation(method);
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
              buildErrorResponse("Unable to authenticate user"));
          }

          // put the user into the context if he has the required permission
          if (validatePermission(authResult.result().user(), permission)) {
            return authResult.result();
          }

          throw new AnnotationHttpHandleException(
            path,
            "User has not the required permission to send a request",
            HttpResponseCode.FORBIDDEN,
            buildErrorResponse("Missing required permission"));
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
          return handleAuthResult(ctx, authResult, HttpSession::user, permission);
        };
      }

      // ok, nothing to do
      return null;
    }
  }
}
