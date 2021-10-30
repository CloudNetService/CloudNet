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

import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import de.dytanic.cloudnet.http.ticket.WebSocketTicket;

public class V2HttpHandlerWebSocketTicket extends V2HttpHandler {

  public V2HttpHandlerWebSocketTicket(String requiredPermission) {
    super(requiredPermission, "POST");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    WebSocketTicket ticket = this.authentication.getWebSocketTicketManager().issueTicket(context.request(), session);
    this.ok(context)
      .body(
        this.success().append("id", ticket.getFullId()).append("expire", ticket.getExpirationTimestamp()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }
}
