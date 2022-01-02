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

package eu.cloudnetservice.cloudnet.node.http.ticket;

import eu.cloudnetservice.cloudnet.common.StringUtil;
import eu.cloudnetservice.cloudnet.common.encrypt.EncryptTo;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpRequest;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class MemoryWebSocketTicketManager implements WebSocketTicketManager {

  public static final long DEFAULT_TICKET_TIMEOUT = 10_000;
  public static final WebSocketTicketManager INSTANCE = new MemoryWebSocketTicketManager();

  private final Map<String, WebSocketTicket> tickets = new ConcurrentHashMap<>();

  @Override
  public @NonNull Collection<WebSocketTicket> tickets() {
    this.removeOutdatedEntries();
    return Collections.unmodifiableCollection(this.tickets.values());
  }

  @Override
  public @Nullable WebSocketTicket expireTicket(@NonNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.remove(this.convertTicketId(ticketId));
  }

  @Override
  public @Nullable WebSocketTicket findTicket(@NonNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.get(this.convertTicketId(ticketId));
  }

  @Override
  public @Nullable WebSocketTicket findAndRemoveTicket(@NonNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.remove(this.convertTicketId(ticketId));
  }

  @Override
  public @NonNull WebSocketTicket issueTicket(@NonNull HttpRequest request, @NonNull HttpSession httpSession) {
    return this.issueTicket(request, httpSession, DEFAULT_TICKET_TIMEOUT);
  }

  @Override
  public @NonNull WebSocketTicket issueTicket(
    @NonNull HttpRequest request,
    @NonNull HttpSession session,
    long timeout
  ) {
    var ticket = new WebSocketTicket(
      StringUtil.generateRandomString(32),
      request.context().channel().clientAddress().host(),
      System.currentTimeMillis() + timeout,
      session
    );
    this.tickets.put(this.convertTicketId(ticket.fullId()), ticket);
    return ticket;
  }

  private @NonNull String convertTicketId(@NonNull String tickedId) {
    return new String(EncryptTo.encryptToSHA256(tickedId), StandardCharsets.UTF_8);
  }

  private void removeOutdatedEntries() {
    for (var entry : this.tickets.entrySet()) {
      if (entry.getValue().expired()) {
        this.tickets.remove(entry.getKey());
      }
    }
  }
}
