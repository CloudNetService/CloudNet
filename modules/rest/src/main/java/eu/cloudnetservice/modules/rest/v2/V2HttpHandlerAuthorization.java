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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.RestConfiguration;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BasicAuth;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

@Singleton
@ApplyHeaders
public final class V2HttpHandlerAuthorization extends V2HttpHandler {

  private final V2HttpAuthentication authentication;
  private final RestConfiguration restConfiguration;

  @Inject
  public V2HttpHandlerAuthorization(@NonNull Configuration configuration, @NonNull V2HttpAuthentication authentication) {
    this.restConfiguration = configuration.restConfiguration();
    this.authentication = authentication;
  }

  @HttpRequestHandler(paths = "/api/v2/auth")
  private void handleWithoutAuth(@NonNull HttpContext ctx) {
    this.response(ctx, HttpResponseCode.UNAUTHORIZED)
      .header("WWW-Authenticate", "Basic realm=\"CloudNet Rest\"")
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @HttpRequestHandler(paths = "/api/v2/auth", methods = "POST")
  private void handleWithBasicAuth(@NonNull HttpContext ctx, @NonNull @BasicAuth PermissionUser user) {
    var jwt = this.authentication.createJwt(
      user,
      TimeUnit.MINUTES.toMillis(this.restConfiguration.jwtValidTimeMinutes()));
    this.ok(ctx)
      .body(this.success().append("token", jwt).append("id", user.uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @HttpRequestHandler(paths = "/api/v2/auth", methods = "POST")
  private void handleWithBearerAuth(@NonNull HttpContext ctx, @NonNull @BearerAuth HttpSession session) {
    this.ok(ctx)
      .body(this.success().append("id", session.user().uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }
}
