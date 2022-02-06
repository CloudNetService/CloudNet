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
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerWebSocketTicket extends V2HttpHandler {

  public V2HttpHandlerWebSocketTicket(@Nullable String requiredPermission) {
    super(requiredPermission, "POST");
  }

  @Override
  protected void handleBearerAuthorized(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull HttpSession session
  ) {
    var ticket = this.authentication.webSocketTicketManager().issueTicket(context.request(), session);
    this.ok(context)
      .body(
        this.success().append("id", ticket.fullId()).append("expire", ticket.expirationTimestamp()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }
}
