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

package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import lombok.NonNull;

public final class PacketAuthorizationResponseListener implements PacketListener {

  private final Lock lock;
  private final Condition condition;

  private volatile boolean result;

  public PacketAuthorizationResponseListener(@NonNull Lock lock, @NonNull Condition condition) {
    this.lock = lock;
    this.condition = condition;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // read the auth result
    this.result = packet.content().readBoolean();
    // signal all listeners waiting for the auth
    try {
      this.lock.lock();
      this.condition.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  public boolean wasAuthSuccessful() {
    return this.result;
  }
}
