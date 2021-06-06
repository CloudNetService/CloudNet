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

package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketSession;
import java.io.InputStream;

public class ChunkedQueryResponse {

  private final ChunkedPacketSession session;
  private final ChunkedPacket beginPacket;
  private final ChunkedPacket endPacket;
  private final InputStream inputStream;

  public ChunkedQueryResponse(ChunkedPacketSession session, ChunkedPacket beginPacket, ChunkedPacket endPacket,
    InputStream inputStream) {
    this.session = session;
    this.beginPacket = beginPacket;
    this.endPacket = endPacket;
    this.inputStream = inputStream;
  }

  public ChunkedPacketSession getSession() {
    return this.session;
  }

  public ChunkedPacket getBeginPacket() {
    return this.beginPacket;
  }

  public ChunkedPacket getEndPacket() {
    return this.endPacket;
  }

  public InputStream getInputStream() {
    return this.inputStream;
  }
}
