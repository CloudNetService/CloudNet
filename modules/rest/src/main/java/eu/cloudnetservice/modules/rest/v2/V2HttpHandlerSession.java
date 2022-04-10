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

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpHandler;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

public class V2HttpHandlerSession extends V2HttpHandler {

  public V2HttpHandlerSession() {
    super(null, "POST");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
    if (path.startsWith("/api/v2/session/logout")) {
      this.handleLogout(context, ses);
    } else if (path.startsWith("/api/v2/session/refresh")) {
      this.handleRefresh(context, ses);
    } else {
      this.response(context, HttpResponseCode.NOT_FOUND).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleRefresh(@NonNull HttpContext context, @NonNull HttpSession session) {
    var jwt = this.authentication.refreshJwt(session, TimeUnit.HOURS.toMillis(1));
    this.ok(context)
      .body(this.success().append("token", jwt).append("uniqueId", session.user().uniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleLogout(@NonNull HttpContext context, @NonNull HttpSession session) {
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
