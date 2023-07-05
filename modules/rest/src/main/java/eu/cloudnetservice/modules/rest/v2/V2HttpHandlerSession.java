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
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.RestConfiguration;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

@Singleton
@ApplyHeaders
public final class V2HttpHandlerSession extends V2HttpHandler {

  private final RestConfiguration restConfiguration;

  @Inject
  public V2HttpHandlerSession(@NonNull Configuration configuration) {
    this.restConfiguration = configuration.restConfiguration();
  }

  @HttpRequestHandler(paths = "/api/v2/session/refresh", methods = "POST")
  private void handleRefresh(@NonNull HttpContext context, @NonNull @BearerAuth HttpSession session) {
    var jwt = session.issuer().refreshJwt(
      session,
      TimeUnit.MINUTES.toMillis(this.restConfiguration.jwtValidTimeMinutes()));
    this.ok(context)
      .body(this.success().append("token", jwt).append("uniqueId", session.user().uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @HttpRequestHandler(paths = "/api/v2/session/logout", methods = "POST")
  private void handleLogout(@NonNull HttpContext context, @NonNull @BearerAuth HttpSession session) {
    if (session.issuer().expireSession(session)) {
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.send403(context, "Unable to close unknown session");
    }
  }
}
