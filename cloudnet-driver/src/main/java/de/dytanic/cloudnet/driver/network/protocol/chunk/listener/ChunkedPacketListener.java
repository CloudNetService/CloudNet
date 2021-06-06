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

package de.dytanic.cloudnet.driver.network.protocol.chunk.listener;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacket;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public abstract class ChunkedPacketListener implements IPacketListener {

  private final Lock lock = new ReentrantLock();
  private final Map<UUID, ChunkedPacketSession> sessions = new HashMap<>();

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    this.lock.lock();
    try {
      ChunkedPacket chunk = ChunkedPacket
        .createIncomingPacket(packet.getChannel(), packet.getUniqueId(), packet.getHeader(), packet.getBuffer())
        .readBuffer();
      if (!this.sessions.containsKey(packet.getUniqueId())) {
        this.sessions.put(packet.getUniqueId(), this.createSession(channel, packet.getUniqueId(), new HashMap<>()));
      }

      this.sessions.get(packet.getUniqueId()).handleIncomingChunk(chunk);
    } finally {
      this.lock.unlock();
    }
  }

  public @NotNull Map<UUID, ChunkedPacketSession> getSessions() {
    return this.sessions;
  }

  @NotNull
  protected ChunkedPacketSession createSession(@NotNull INetworkChannel channel, @NotNull UUID sessionUniqueId,
    @NotNull Map<String, Object> properties) throws IOException {
    return new ChunkedPacketSession(channel, this, sessionUniqueId,
      this.createOutputStream(sessionUniqueId, properties), properties);
  }

  @NotNull
  protected abstract OutputStream createOutputStream(@NotNull UUID sessionUniqueId,
    @NotNull Map<String, Object> properties) throws IOException;

  protected void handleComplete(@NotNull ChunkedPacketSession session) throws IOException {
  }
}
