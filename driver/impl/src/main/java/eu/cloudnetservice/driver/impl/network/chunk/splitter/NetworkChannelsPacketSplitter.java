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

package eu.cloudnetservice.driver.impl.network.chunk.splitter;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import java.util.Collection;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A default implementation of a chunked packet splitter, splitting each packet chunk to multiple channels.
 *
 * @since 4.0
 */
public record NetworkChannelsPacketSplitter(@NonNull Collection<NetworkChannel> channels) implements Consumer<Packet> {

  /**
   * Sends the given packet safely to all listening components of the chunked data transfer.
   *
   * @param packet the packet to send.
   * @throws NullPointerException if the given packet is null.
   */
  @Override
  public void accept(@NonNull Packet packet) {
    var packetContent = packet.content();
    for (var channel : this.channels) {
      try {
        // acquire the packet here to prevent releasing of the content when the packet gets serialized
        // for actual sending into the network. this also implicitly removes our need to release the packet
        // in the finally block - it's already done at that point
        packetContent.acquire().startTransaction();
        channel.sendPacketSync(packet);
      } finally {
        packetContent.redoTransaction();
      }
    }

    // force release the packet content, in case something went wrong
    packetContent.forceRelease();
  }
}
