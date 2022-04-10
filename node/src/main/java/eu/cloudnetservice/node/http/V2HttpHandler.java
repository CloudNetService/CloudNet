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

package eu.cloudnetservice.node.http;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import eu.cloudnetservice.driver.network.http.HttpRequest;
import eu.cloudnetservice.driver.network.http.HttpResponse;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.RestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class V2HttpHandler implements HttpHandler {

  protected static final Logger LOGGER = LogManager.logger(V2HttpHandler.class);
  protected static final V2HttpAuthentication DEFAULT_AUTH = new V2HttpAuthentication();

  protected final String requiredPermission;
  protected final Set<String> requestMethods;
  protected final String requestMethodsString;

  protected final V2HttpAuthentication authentication;
  protected final RestConfiguration restConfiguration;

  public V2HttpHandler(@Nullable String requiredPermission, @NonNull String... requestMethods) {
    this(requiredPermission, DEFAULT_AUTH, Node.instance().config().restConfiguration(), requestMethods);
  }

  public V2HttpHandler(
    @Nullable String requiredPermission,
    @NonNull V2HttpAuthentication authentication,
    @NonNull RestConfiguration restConfiguration,
    @NonNull String... requestMethods
  ) {
    this.requiredPermission = requiredPermission;
    this.authentication = authentication;
    this.restConfiguration = restConfiguration;

    this.requestMethods = Set.of(requestMethods);
    this.requestMethodsString = requestMethods.length == 0 ? "*" : String.join(", ", requestMethods);
  }

  @Override
  public void handle(@NonNull String path, @NonNull HttpContext context) throws Exception {
    if (context.request().method().equalsIgnoreCase("OPTIONS")) {
      this.sendOptions(context);
    } else {
      if (!this.requestMethods.isEmpty() && !this.requestMethods.contains(context.request().method().toUpperCase())) {
        this.response(context, HttpResponseCode.METHOD_NOT_ALLOWED)
          .header("Allow", this.requestMethodsString)
          .context()
          .cancelNext(true)
          .closeAfter();
      } else if (context.request().hasHeader("Authorization")) {
        // try the more often used bearer auth first
        var session = this.authentication.handleBearerLoginRequest(context.request());
        if (session.succeeded()) {
          if (this.testPermission(session.result().user(), context.request())) {
            this.handleBearerAuthorized(path, context, session.result());
          } else {
            this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
          }
          return;
        } else if (session.hasErrorMessage()) {
          this.send401(context, session.errorMessage());
          return;
        }
        // try the basic auth method
        var basic = this.authentication.handleBasicLoginRequest(context.request());
        if (basic.succeeded()) {
          if (this.testPermission(basic.result(), context.request())) {
            this.handleBasicAuthorized(path, context, basic.result());
          } else {
            this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
          }
          return;
        } else if (basic.hasErrorMessage()) {
          this.send401(context, basic.errorMessage());
          return;
        }
        // send an unauthorized response
        this.send401(context, "No supported authentication method provided. Supported: Basic, Bearer");
      } else {
        // there was no authorization given, try without one
        this.handleUnauthorized(path, context);
      }
    }
  }

  protected void handleUnauthorized(@NonNull String path, @NonNull HttpContext context) throws Exception {
    this.send401(context, "Authentication required");
  }

  protected void handleBasicAuthorized(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull PermissionUser user
  ) {
  }

  protected void handleBearerAuthorized(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull HttpSession session
  ) {
  }

  protected boolean testPermission(@NonNull PermissionUser user, @NonNull HttpRequest request) {
    if (this.requiredPermission == null || this.requiredPermission.isEmpty()) {
      return true;
    } else {
      return CloudNetDriver.instance().permissionManagement().hasPermission(
        user,
        Permission.of(this.requiredPermission + '.' + request.method().toLowerCase()));
    }
  }

  protected void send403(@NonNull HttpContext context, @NonNull String reason) {
    this.response(context, HttpResponseCode.FORBIDDEN)
      .body(this.failure().append("reason", reason).toString().getBytes(StandardCharsets.UTF_8))
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void send401(@NonNull HttpContext context, @NonNull String reason) {
    this.response(context, HttpResponseCode.UNAUTHORIZED)
      .body(this.failure().append("reason", reason).toString().getBytes(StandardCharsets.UTF_8))
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void sendOptions(@NonNull HttpContext context) {
    context
      .cancelNext(true)
      .response()
      .status(HttpResponseCode.OK)
      .header("Access-Control-Allow-Credentials", "true")
      .header("Access-Control-Allow-Methods", this.requestMethodsString)
      .header("Access-Control-Allow-Origin", this.restConfiguration.corsPolicy())
      .header("Access-Control-Allow-Headers", this.restConfiguration.allowedHeaders())
      .header("Access-Control-Expose-Headers", this.restConfiguration.exposedHeaders())
      .header("Access-Control-Max-Age", Integer.toString(this.restConfiguration.accessControlMaxAge()));
  }

  protected @NonNull HttpResponse ok(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.OK);
  }

  protected @NonNull HttpResponse badRequest(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.BAD_REQUEST);
  }

  protected @NonNull HttpResponse notFound(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.NOT_FOUND);
  }

  protected @NonNull HttpResponse response(@NonNull HttpContext context, @NonNull HttpResponseCode statusCode) {
    return context.response()
      .status(statusCode)
      .header("Content-Type", "application/json")
      .header("Access-Control-Allow-Origin", this.restConfiguration.corsPolicy());
  }

  protected @NonNull JsonDocument body(@NonNull HttpRequest request) {
    return JsonDocument.fromJsonBytes(request.body());
  }

  protected @NonNull JsonDocument success() {
    return JsonDocument.newDocument("success", true);
  }

  protected @NonNull JsonDocument failure() {
    return JsonDocument.newDocument("success", false);
  }

  protected @NonNull Node node() {
    return Node.instance();
  }

  protected @NonNull Configuration configuration() {
    return this.node().config();
  }
}
