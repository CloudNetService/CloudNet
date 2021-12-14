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
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import java.util.concurrent.TimeUnit;

public class V2HttpHandlerAuthorization extends V2HttpHandler {

  public V2HttpHandlerAuthorization() {
    super(null, "POST");
  }

  @Override
  protected void handleUnauthorized(String path, IHttpContext context) {
    this.response(context, HttpResponseCode.HTTP_UNAUTHORIZED)
      .header("WWW-Authenticate", "Basic realm=\"CloudNet Rest\"")
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  @Override
  protected void handleBasicAuthorized(String path, IHttpContext context, PermissionUser user) {
    var jwt = this.authentication.createJwt(user, TimeUnit.HOURS.toMillis(1)); // todo: configurable
    this.ok(context)
      .body(this.success().append("token", jwt).append("id", user.getUniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    this.ok(context)
      .body(this.success().append("id", session.getUser().getUniqueId()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }
}
