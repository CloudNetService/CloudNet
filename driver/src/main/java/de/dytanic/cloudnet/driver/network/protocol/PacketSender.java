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

package de.dytanic.cloudnet.driver.network.protocol;

import lombok.NonNull;

/**
 * Represents a sender of packets who can receive packets too.
 */
public interface PacketSender {

  /**
   * Sends a packet to this sender.
   *
   * @param packet the packet to send.
   */
  void sendPacket(@NonNull Packet packet);

  /**
   * Sends a packet to this sender and blocks until the packet was flushed into the channel.
   *
   * @param packet the packet to send.
   */
  void sendPacketSync(@NonNull Packet packet);

  /**
   * Sends all given packets to this sender.
   *
   * @param packets the packets to send.
   * @see #sendPacket(Packet)
   */
  default void sendPacket(@NonNull Packet... packets) {
    for (var packet : packets) {
      this.sendPacket(packet);
    }
  }

  /**
   * Sends all given packets to this sender and waits until the packets were flushed into the channel.
   *
   * @param packets the packets to send.
   * @see #sendPacketSync(Packet)
   */
  default void sendPacketSync(@NonNull Packet... packets) {
    for (var packet : packets) {
      this.sendPacketSync(packet);
    }
  }
}
