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

package eu.cloudnetservice.cloudnet.node.http;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.node.config.AccessControlConfiguration;
import java.util.function.BiPredicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WebSocketAbleV2HttpHandler extends V2HttpHandler {

  protected final BiPredicate<HttpContext, String> handlingTester;

  public WebSocketAbleV2HttpHandler(
    @Nullable String requiredPermission,
    @NonNull BiPredicate<HttpContext, String> handlingTester,
    @NonNull String... supportedRequestMethods
  ) {
    super(requiredPermission, supportedRequestMethods);
    this.handlingTester = handlingTester;
  }

  public WebSocketAbleV2HttpHandler(
    @Nullable String requiredPermission,
    @NonNull V2HttpAuthentication authentication,
    @NonNull AccessControlConfiguration accessControlConfiguration,
    @NonNull BiPredicate<HttpContext, String> handlingTester,
    @NonNull String... supportedRequestMethods
  ) {
    super(requiredPermission, authentication, accessControlConfiguration, supportedRequestMethods);
    this.handlingTester = handlingTester;
  }

  @Override
  protected final void handleUnauthorized(@NonNull String path, @NonNull HttpContext context) throws Exception {
    if (!this.handlingTester.test(context, path)) {
      this.handleUnauthorizedRequest(path, context);
      return;
    }

    var ticketIds = context.request().queryParameters().get("ticket");
    var ticketId = ticketIds == null ? null : Iterables.getFirst(ticketIds, null);
    if (ticketId == null) {
      this.send403(context, "Missing authorization or ticket information");
      return;
    }

    var ticket = this.authentication.webSocketTicketManager().findAndRemoveTicket(ticketId);
    if (ticket == null || !ticket.requestingIp().equals(context.channel().clientAddress().host())) {
      this.send403(context, "Invalid ticket id or ticket not issued for client");
      return;
    }

    this.handleTicketAuthorizedRequest(path, context, ticket.associatedSession());
  }

  protected void handleUnauthorizedRequest(@NonNull String path, @NonNull HttpContext context) {
    this.send403(context, "Authentication required");
  }

  protected abstract void handleTicketAuthorizedRequest(@NonNull String path, @NonNull HttpContext context,
    @NonNull HttpSession session);
}
