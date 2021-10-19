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

package de.dytanic.cloudnet.http.ticket;

import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MemoryWebSocketTicketManager implements WebSocketTicketManager {

  public static final long DEFAULT_TICKET_TIMEOUT = 10_000;
  public static final WebSocketTicketManager INSTANCE = new MemoryWebSocketTicketManager();

  private final Map<String, WebSocketTicket> tickets = new ConcurrentHashMap<>();

  @Override
  public @NotNull Collection<WebSocketTicket> getTickets() {
    this.removeOutdatedEntries();
    return Collections.unmodifiableCollection(this.tickets.values());
  }

  @Override
  public @Nullable WebSocketTicket expireTicket(@NotNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.remove(this.convertTicketId(ticketId));
  }

  @Override
  public @Nullable WebSocketTicket findTicket(@NotNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.get(this.convertTicketId(ticketId));
  }

  @Override
  public @Nullable WebSocketTicket findAndRemoveTicket(@NotNull String ticketId) {
    this.removeOutdatedEntries();
    return this.tickets.remove(this.convertTicketId(ticketId));
  }

  @Override
  public @NotNull WebSocketTicket issueTicket(@NotNull IHttpRequest request, @NotNull HttpSession httpSession) {
    return this.issueTicket(request, httpSession, DEFAULT_TICKET_TIMEOUT);
  }

  @Override
  public @NotNull WebSocketTicket issueTicket(
    @NotNull IHttpRequest request,
    @NotNull HttpSession session,
    long timeout
  ) {
    WebSocketTicket ticket = new WebSocketTicket(
      StringUtil.generateRandomString(32),
      request.context().channel().clientAddress().getHost(),
      System.currentTimeMillis() + timeout,
      session
    );
    this.tickets.put(this.convertTicketId(ticket.getFullId()), ticket);
    return ticket;
  }

  private @NotNull String convertTicketId(@NotNull String tickedId) {
    return new String(EncryptTo.encryptToSHA256(tickedId), StandardCharsets.UTF_8);
  }

  private void removeOutdatedEntries() {
    for (Entry<String, WebSocketTicket> entry : this.tickets.entrySet()) {
      if (entry.getValue().isExpired()) {
        this.tickets.remove(entry.getKey());
      }
    }
  }
}
