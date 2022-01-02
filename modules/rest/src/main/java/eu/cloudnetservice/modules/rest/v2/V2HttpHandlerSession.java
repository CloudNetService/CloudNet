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
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.concurrent.TimeUnit;

public class V2HttpHandlerSession extends V2HttpHandler {

  public V2HttpHandlerSession() {
    super(null, "POST");
  }

  @Override
  protected void handleBearerAuthorized(String path, HttpContext context, HttpSession session) {
    if (path.startsWith("/api/v2/session/logout")) {
      this.handleLogout(context, session);
    } else if (path.startsWith("/api/v2/session/refresh")) {
      this.handleRefresh(context, session);
    } else {
      this.response(context, HttpResponseCode.HTTP_NOT_FOUND).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleRefresh(HttpContext context, HttpSession session) {
    var jwt = this.authentication.refreshJwt(session, TimeUnit.HOURS.toMillis(1));
    this.ok(context)
      .body(this.success().append("token", jwt).append("uniqueId", session.user().uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleLogout(HttpContext context, HttpSession session) {
    if (this.authentication.expireSession(session)) {
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.send403(context, "Unable to close unknown session");
    }
  }
}
