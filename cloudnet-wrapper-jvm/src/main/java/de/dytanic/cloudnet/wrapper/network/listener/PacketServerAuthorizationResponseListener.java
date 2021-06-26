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

package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PacketServerAuthorizationResponseListener implements IPacketListener {

  private final ReentrantLock lock;

  private final Condition condition;

  private boolean result;

  public PacketServerAuthorizationResponseListener(ReentrantLock lock, Condition condition) {
    this.lock = lock;
    this.condition = condition;
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (packet.getHeader().contains("access") && packet.getHeader().contains("text")) {
      this.result = packet.getHeader().getBoolean("access");

      try {
        this.lock.lock();
        this.condition.signalAll();
      } finally {
        this.lock.unlock();
      }
    }
  }

  public boolean isResult() {
    return this.result;
  }
}
