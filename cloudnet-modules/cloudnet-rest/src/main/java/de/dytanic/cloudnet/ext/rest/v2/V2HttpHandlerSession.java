/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import java.util.concurrent.TimeUnit;

public class V2HttpHandlerSession extends V2HttpHandler {

  public V2HttpHandlerSession() {
    super(null, "POST");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (path.startsWith("/api/v2/session/logout")) {
      this.handleLogout(context, session);
    } else if (path.startsWith("/api/v2/session/refresh")) {
      this.handleRefresh(context, session);
    } else {
      this.response(context, HttpResponseCode.HTTP_NOT_FOUND).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleRefresh(IHttpContext context, HttpSession session) {
    String jwt = this.authentication.refreshJwt(session, TimeUnit.HOURS.toMillis(1));
    this.ok(context)
      .body(this.success().append("token", jwt).append("uniqueId", session.getUser().getUniqueId()).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleLogout(IHttpContext context, HttpSession session) {
    if (this.authentication.expireSession(session)) {
      this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
    } else {
      this.send403(context, "Unable to close unknown session");
    }
  }
}
