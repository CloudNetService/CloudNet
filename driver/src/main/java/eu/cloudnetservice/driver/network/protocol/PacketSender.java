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

package eu.cloudnetservice.driver.network.protocol;

import lombok.NonNull;

/**
 * Represents a source of packets which is connected to a specific target.
 *
 * @since 4.0
 */
public interface PacketSender {

  /**
   * Sends the given packet to the associated target.
   *
   * @param packet the packet to send.
   * @throws NullPointerException if the packet is null.
   */
  void sendPacket(@NonNull Packet packet);

  /**
   * Sends the given packet to the associated target, waiting for the packet write to the channel to complete before
   * resuming the current thread.
   *
   * @param packet the packet to send.
   * @throws NullPointerException if the given packet is null.
   */
  void sendPacketSync(@NonNull Packet packet);

  /**
   * Sends all the given packets to the associated target.
   *
   * @param packets the packets to send.
   * @throws NullPointerException if the packets are null.
   */
  default void sendPacket(@NonNull Packet... packets) {
    for (var packet : packets) {
      this.sendPacket(packet);
    }
  }

  /**
   * Sends all the given packets to the associated target, waiting for the packet writes to the channel to complete
   * before resuming the current thread.
   *
   * @param packets the packets to send.
   * @throws NullPointerException if the packets are null.
   */
  default void sendPacketSync(@NonNull Packet... packets) {
    for (var packet : packets) {
      this.sendPacketSync(packet);
    }
  }
}
