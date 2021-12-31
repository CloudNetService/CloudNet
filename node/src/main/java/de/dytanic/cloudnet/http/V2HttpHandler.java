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

package de.dytanic.cloudnet.http;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.config.Configuration;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpContext;
import de.dytanic.cloudnet.driver.network.http.HttpHandler;
import de.dytanic.cloudnet.driver.network.http.HttpRequest;
import de.dytanic.cloudnet.driver.network.http.HttpResponse;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.NonNull;

public abstract class V2HttpHandler implements HttpHandler {

  protected static final V2HttpAuthentication DEFAULT_AUTH = new V2HttpAuthentication();

  protected static final Logger LOGGER = LogManager.logger(V2HttpHandler.class);

  protected final String requiredPermission;
  protected final String[] supportedRequestMethods;
  protected final String supportedRequestMethodsString;

  protected final V2HttpAuthentication authentication;
  protected final AccessControlConfiguration accessControlConfiguration;

  public V2HttpHandler(String requiredPermission, String... supportedRequestMethods) {
    this(requiredPermission, DEFAULT_AUTH, AccessControlConfiguration.defaults(), supportedRequestMethods);
  }

  public V2HttpHandler(String requiredPermission, V2HttpAuthentication authentication,
    AccessControlConfiguration accessControlConfiguration, String... supportedRequestMethods) {
    this.requiredPermission = requiredPermission;
    this.authentication = authentication;
    this.accessControlConfiguration = accessControlConfiguration;

    this.supportedRequestMethods = supportedRequestMethods;
    // needed to use a binary search later
    Arrays.sort(this.supportedRequestMethods);
    this.supportedRequestMethodsString = supportedRequestMethods.length == 0
      ? "*" : String.join(", ", supportedRequestMethods);
  }

  @Override
  public void handle(@NonNull String path, @NonNull HttpContext context) throws Exception {
    if (context.request().method().equalsIgnoreCase("OPTIONS")) {
      this.sendOptions(context);
    } else {
      if (this.supportedRequestMethods.length > 0
        && Arrays.binarySearch(this.supportedRequestMethods, context.request().method().toUpperCase()) < 0) {
        this.response(context, HttpResponseCode.HTTP_BAD_METHOD)
          .header("Allow", this.supportedRequestMethodsString)
          .context()
          .cancelNext(true)
          .closeAfter();
      } else if (context.request().hasHeader("Authorization")) {
        // try the more often used bearer auth first
        var session = this.authentication
          .handleBearerLoginRequest(context.request());
        if (session.succeeded()) {
          if (this.testPermission(session.result().user(), context.request())) {
            this.handleBearerAuthorized(path, context, session.result());
          } else {
            this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
          }
          return;
        } else if (session.hasErrorMessage()) {
          this.send403(context, session.errorMessage());
          return;
        }
        // try the basic auth method
        var user = this.authentication
          .handleBasicLoginRequest(context.request());
        if (user.succeeded()) {
          if (this.testPermission(user.result(), context.request())) {
            this.handleBasicAuthorized(path, context, user.result());
          } else {
            this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
          }
          return;
        } else if (user.hasErrorMessage()) {
          this.send403(context, user.errorMessage());
          return;
        }
        // send an unauthorized response
        this.send403(context, "No supported authentication method provided. Supported: Basic, Bearer");
      } else {
        // there was no authorization given, try without one
        this.handleUnauthorized(path, context);
      }
    }
  }

  protected void handleUnauthorized(String path, HttpContext context) throws Exception {
    this.send403(context, "Authentication required");
  }

  protected void handleBasicAuthorized(String path, HttpContext context, PermissionUser user) {
  }

  protected void handleBearerAuthorized(String path, HttpContext context, HttpSession session) {
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

  protected void send403(HttpContext context, String reason) {
    this.response(context, HttpResponseCode.HTTP_FORBIDDEN)
      .body(this.failure().append("reason", reason).toString().getBytes(StandardCharsets.UTF_8))
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void sendOptions(HttpContext context) {
    context
      .cancelNext(true)
      .response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Access-Control-Max-Age", Integer.toString(this.accessControlConfiguration.accessControlMaxAge()))
      .header("Access-Control-Allow-Origin", this.accessControlConfiguration.corsPolicy())
      .header("Access-Control-Allow-Headers", "*")
      .header("Access-Control-Expose-Headers", "Accept, Origin, if-none-match, Access-Control-Allow-Headers, " +
        "Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
      .header("Access-Control-Allow-Credentials", "true")
      .header("Access-Control-Allow-Methods", this.supportedRequestMethodsString);
  }

  protected HttpResponse ok(HttpContext context) {
    return this.response(context, HttpResponseCode.HTTP_OK);
  }

  protected HttpResponse badRequest(HttpContext context) {
    return this.response(context, HttpResponseCode.HTTP_BAD_REQUEST);
  }

  protected HttpResponse notFound(HttpContext context) {
    return this.response(context, HttpResponseCode.HTTP_NOT_FOUND);
  }

  protected HttpResponse response(HttpContext context, int statusCode) {
    return context.response()
      .statusCode(statusCode)
      .header("Content-Type", "application/json")
      .header("Access-Control-Allow-Origin", this.accessControlConfiguration.corsPolicy());
  }

  protected JsonDocument body(@NonNull HttpRequest request) {
    return JsonDocument.fromJsonBytes(request.body());
  }

  protected JsonDocument success() {
    return JsonDocument.newDocument("success", true);
  }

  protected JsonDocument failure() {
    return JsonDocument.newDocument("success", false);
  }

  protected CloudNet node() {
    return CloudNet.instance();
  }

  protected Configuration configuration() {
    return this.node().config();
  }
}
