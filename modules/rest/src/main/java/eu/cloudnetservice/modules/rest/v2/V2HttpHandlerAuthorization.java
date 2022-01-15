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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionUser;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class V2HttpHandlerAuthorization extends V2HttpHandler {

  public V2HttpHandlerAuthorization() {
    super(null, "POST");
  }

  @Override
  protected void handleUnauthorized(@NotNull String path, @NotNull HttpContext context) {
    this.response(context, HttpResponseCode.UNAUTHORIZED)
      .header("WWW-Authenticate", "Basic realm=\"CloudNet Rest\"")
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  @Override
  protected void handleBasicAuthorized(@NotNull String path, @NotNull HttpContext con, @NotNull PermissionUser user) {
    var jwt = this.authentication.createJwt(user, TimeUnit.HOURS.toMillis(1)); // todo: configurable
    this.ok(con)
      .body(this.success().append("token", jwt).append("id", user.uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  @Override
  protected void handleBearerAuthorized(@NotNull String path, @NotNull HttpContext context,
    @NotNull HttpSession session) {
    this.ok(context)
      .body(this.success().append("id", session.user().uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }
}
