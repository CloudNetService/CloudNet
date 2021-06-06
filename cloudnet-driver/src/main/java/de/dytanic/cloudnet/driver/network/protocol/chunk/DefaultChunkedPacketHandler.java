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

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import java.util.Collection;
import java.util.function.Consumer;

public class DefaultChunkedPacketHandler {

  public static Consumer<ChunkedPacket> createHandler(Collection<INetworkChannel> channels) {
    return packet -> {
      for (INetworkChannel channel : channels) {
        if (!channel.isActive()) {
          if (noneActive(channels)) {
            throw ChunkInterrupt.INSTANCE;
          }
          continue;
        }

        if (!waitWritable(channel)) {
          continue;
        }

        channel.sendPacketSync(packet.fillBuffer());
      }

      packet.clearData();
    };
  }

  private static boolean noneActive(Collection<INetworkChannel> channels) {
    for (INetworkChannel channel : channels) {
      if (channel.isActive()) {
        return false;
      }
    }
    return true;
  }

  private static boolean waitWritable(INetworkChannel channel) {
    while (!channel.isWriteable()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException exception) {
        throw ChunkInterrupt.INSTANCE;
      }

      if (!channel.isActive()) {
        return false;
      }
    }

    return true;
  }

}
