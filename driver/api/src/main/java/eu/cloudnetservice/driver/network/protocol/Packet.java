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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.time.Instant;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the main communication entrypoint for the CloudNet network. Every data which is sent over the network must
 * implement this class in order to indicate that. A packet contains 3 objects, 2 of them are required:
 * <ol>
 *   <li>The channel id of the channel to send the packet to, for later identification. (Required)
 *   <li>The data buf, representing the content of the packet which gets transferred. (Required)
 *   <li>The query unique id for identification of the query when a network component responds. (Optional)
 * </ol>
 * <p>
 * A packet channel id must be unique within the network and represents the first identification point when receiving a
 * packet. Each listener for packets, registered via the packet listener registry, uses the id to identify all listeners
 * which must get called in order to process the packet. Later identification in more detail can be made by reading from
 * the content of the buffer.
 * <p>
 * The query unique id of the packet is only set in two cases, either if the packet sender expects a response from the
 * component to which the packet got sent, or if the packet is a response to a query, in which case the packet unique id
 * should be -1.
 *
 * @since 4.0
 */
public interface Packet {

  /**
   * Get a jvm static implementation of a packet which holds no content and is designed to catch read/write activity on
   * those packets by throwing an exception. Normally used by the query manager to complete futures which timed out.
   *
   * @return a jvm static instance of an empty packet.
   */
  static @NonNull Packet empty() {
    return EmptyPacket.INSTANCE;
  }

  /**
   * Constructs a new packet as a response to this packet. This is used for query communication where responding to a
   * query is very common. The resulting packet will
   * <ol>
   *   <li>have a packet id set to -1 for query identification.
   *   <li>have the same query unique id as this packet has.
   *   <li>have the given content buffer as the content set.
   * </ol>
   *
   * @param content the content of the response to this packet.
   * @return a new packet representing a response to this packet.
   * @throws NullPointerException if the given content is null.
   */
  @NonNull
  Packet constructResponse(@NonNull DataBuf content);

  /**
   * Get the unique id of this packet. The unique id of the packet is only set when this packet is a query packet and
   * expects a response. In this case the response to this packet should have the packet id set to -1 and the same query
   * unique id as this packet has. Normally a response is constructed by {@link #constructResponse(DataBuf)}.
   *
   * @return the unique id of this packet, or null if this packet is not a query packet.
   */
  @Nullable UUID uniqueId();

  /**
   * Sets the unique id of this packet. If the unique id is set and the packet is sent the packet will be handled as a
   * query packet. This method should not be used directly to set or change the unique id of a packet. The unique id
   * handling is made by the query manager used to send the packet as a query.
   *
   * @param uniqueId the new unique id of this packet or null if this packet is not a query packet.
   */
  @ApiStatus.Internal
  void uniqueId(@Nullable UUID uniqueId);

  /**
   * Get the channel id to which this packet was sent. Listeners can be registered to that channel and will be notified
   * if a packet was received for the specified channel. Each packet id should be unique within the whole network.
   *
   * @return the channel id to which this packet was sent.
   */
  int channel();

  /**
   * Get if this packet is prioritized. If a packet is marked as high priority, it will instantly get handled on the
   * receiving component without getting queued.
   * <p>
   * This option should be used with care, as each thread which will normally read from the channel will be blocked with
   * the packet handling of this packet. Blocking the handler thread too long will cause other packets to be delayed for
   * no visible reason.
   *
   * @return if this packet is prioritized.
   */
  boolean prioritized();

  /**
   * Get if this packet still has readable bytes left. Useful to verify that from a packet can actually be read instead
   * of running into exceptions because the end of the buffer has been reached.
   *
   * @return true if this packet still has data to read, false otherwise.
   */
  boolean readable();

  /**
   * Get the content of this packet. This method call always returns the same buffer to the caller, handling the buffer
   * release and transactional reading is crucial to not run into exceptions when multiple handlers are handling the
   * same packet.
   *
   * @return the content of this packet.
   */
  @NonNull
  DataBuf content();

  /**
   * Get an epoch timestamp of the creation time of this packet. When the packet is created by a decoder, the time will
   * be used as the creation millis. There is no guarantee for this time to be the exact same time as when the packet
   * gets sent to the component, nor when a listener first received the packet.
   *
   * @return the creation timestamp of this packet.
   */
  @NonNull
  Instant creation();
}
