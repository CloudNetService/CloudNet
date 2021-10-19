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

import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.http.HttpSession;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WebSocketTicketManager {

  static @NotNull WebSocketTicketManager memoryCached() {
    return MemoryWebSocketTicketManager.INSTANCE;
  }

  @NotNull Collection<WebSocketTicket> getTickets();

  @Nullable WebSocketTicket expireTicket(@NotNull String ticketId);

  @Nullable WebSocketTicket findTicket(@NotNull String ticketId);

  @Nullable WebSocketTicket findAndRemoveTicket(@NotNull String ticketId);

  @NotNull WebSocketTicket issueTicket(@NotNull IHttpRequest request, @NotNull HttpSession session);

  @NotNull WebSocketTicket issueTicket(@NotNull IHttpRequest request, @NotNull HttpSession session, long timeout);
}
