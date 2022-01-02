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

package eu.cloudnetservice.cloudnet.driver.network.protocol;

import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A packet represents the main communication unit between to network participants within the CloudNet network. This can
 * be between two nodes or between the Wrapper and the node.
 *
 * @see BasePacket
 * @see NetworkChannel
 */
public interface Packet {

  /**
   * Constructs a new packet that holds the same uuid as the packet used to create the response
   *
   * @param content the content of the new packet
   * @return the new constructed packet
   */
  @NonNull Packet constructResponse(@NonNull DataBuf content);

  /**
   * Get the unique id of this packet. This field must not be defined if the packet has no unique id set. If the unique
   * id is set a response to this packet is expected by the client receiving it. Not responding to the packet can lead
   * to internal issues or thread deadlocks.
   *
   * @return the query unique id of the packet or {@code null} if this packet is not a query packet.
   */
  @Nullable UUID uniqueId();

  /**
   * Sets the unique id of this packet. If the unique id is set and the packet is sent the packet will be handled as a
   * query packet.
   *
   * @param uniqueId the unique id of the packet or {@code null} if the packet should not be a query packet.
   */
  void uniqueId(@Nullable UUID uniqueId);

  /**
   * Get the channel id to which this packet was sent. Listeners can be registered to that channel and will be notified
   * if a packet was received for the specified channel.
   *
   * @return the channel id to which this packet was sent.
   */
  int channel();

  /**
   * Get the content of this packet. Data written to the buffer must not be accepted but will never throw an exception.
   * If the packet was sent by a network participant the buffer will never be writable.
   *
   * @return the content of this packet.
   */
  @NonNull DataBuf content();

  /**
   * Get a unix timestamp of the creation milliseconds of this packet. When the packet is created by a decoder, the time
   * will be used as the creation millis.
   *
   * @return the creation milliseconds of this packet.
   */
  long creationMillis();

  /**
   * Defines if there should be a debug message when the packet get encoded, sent, received etc. This setting will be
   * enabled by default but should be disabled for very high rated packets.
   *
   * @return if there should be debug messages for this type of packet
   */
  default boolean showDebug() {
    return true;
  }
}
