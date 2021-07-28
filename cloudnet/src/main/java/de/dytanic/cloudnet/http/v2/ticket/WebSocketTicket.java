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

package de.dytanic.cloudnet.http.v2.ticket;

import de.dytanic.cloudnet.http.v2.HttpSession;

public class WebSocketTicket {

  private final String fullId;
  private final String requestingIp;
  private final long expirationTimestamp;
  private final HttpSession associatedSession;

  public WebSocketTicket(String fullId, String requestingIp, long expirationTimestamp, HttpSession session) {
    this.fullId = fullId;
    this.requestingIp = requestingIp;
    this.expirationTimestamp = expirationTimestamp;
    this.associatedSession = session;
  }

  public String getFullId() {
    return this.fullId;
  }

  public String getRequestingIp() {
    return this.requestingIp;
  }

  public long getExpirationTimestamp() {
    return this.expirationTimestamp;
  }

  public HttpSession getAssociatedSession() {
    return this.associatedSession;
  }

  public boolean isExpired() {
    return System.currentTimeMillis() >= this.expirationTimestamp;
  }
}
