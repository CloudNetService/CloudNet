/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.network.listener;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;

public final class AuthorizationPacketListener implements PacketListener {

  private final Thread blockedThread;
  private final AtomicBoolean result;

  public AuthorizationPacketListener(@NonNull Thread blockedThread) {
    this.blockedThread = blockedThread;
    this.result = new AtomicBoolean(false);
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // read the auth result
    var content = packet.content();
    this.result.setRelease(content.readBoolean());

    // skip the next two booleans from the packet
    content.readBoolean();
    content.readBoolean();

    // signal all listeners waiting for the auth
    LockSupport.unpark(this.blockedThread);
  }

  public boolean wasAuthSuccessful() {
    return this.result.getAcquire();
  }
}
