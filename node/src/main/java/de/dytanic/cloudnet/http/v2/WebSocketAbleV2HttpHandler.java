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

package de.dytanic.cloudnet.http.v2;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.v2.ticket.WebSocketTicket;
import java.util.List;
import java.util.function.BiPredicate;

public abstract class WebSocketAbleV2HttpHandler extends V2HttpHandler {

  protected final BiPredicate<IHttpContext, String> handlingTester;

  public WebSocketAbleV2HttpHandler(String requiredPermission, BiPredicate<IHttpContext, String> handlingTester,
    String... supportedRequestMethods) {
    super(requiredPermission, supportedRequestMethods);
    this.handlingTester = handlingTester;
  }

  public WebSocketAbleV2HttpHandler(
    String requiredPermission, V2HttpAuthentication authentication,
    AccessControlConfiguration accessControlConfiguration, BiPredicate<IHttpContext, String> handlingTester,
    String... supportedRequestMethods
  ) {
    super(requiredPermission, authentication, accessControlConfiguration, supportedRequestMethods);
    this.handlingTester = handlingTester;
  }

  @Override
  protected final void handleUnauthorized(String path, IHttpContext context) throws Exception {
    if (!this.handlingTester.test(context, path)) {
      this.handleUnauthorizedRequest(path, context);
      return;
    }

    List<String> ticketIds = context.request().queryParameters().get("ticket");
    String ticketId = ticketIds == null ? null : Iterables.getFirst(ticketIds, null);
    if (ticketId == null) {
      this.send403(context, "Missing authorization or ticket information");
      return;
    }

    WebSocketTicket ticket = this.authentication.getWebSocketTicketManager().findAndRemoveTicket(ticketId);
    if (ticket == null || !ticket.getRequestingIp().equals(context.channel().clientAddress().getHost())) {
      this.send403(context, "Invalid ticket id or ticket not issued for client");
      return;
    }

    this.handleTicketAuthorizedRequest(path, context, ticket.getAssociatedSession());
  }

  protected void handleUnauthorizedRequest(String path, IHttpContext context) {
    this.send403(context, "Authentication required");
  }

  protected abstract void handleTicketAuthorizedRequest(String path, IHttpContext context, HttpSession session)
      ;
}
