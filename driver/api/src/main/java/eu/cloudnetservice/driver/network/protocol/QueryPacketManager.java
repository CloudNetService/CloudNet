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

import eu.cloudnetservice.driver.network.NetworkChannel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the manager used by the cloud to manage incoming and outgoing query packets.
 *
 * @since 4.0
 */
public interface QueryPacketManager {

  /**
   * Get the network channel this manager is associated with. This channel is used to transmit query packets to the
   * receiver, therefore the network channel of the received must be known in advance.
   *
   * @return the network channel to which this manager belongs.
   */
  @NonNull
  NetworkChannel networkChannel();

  /**
   * Returns the approximate number of query response handlers that are currently awaiting a response. The returned
   * value is an estimate, as the real value might differ due to concurrent sends of queries or receive of responses.
   *
   * @return the estimated number of handlers that are awaiting a response.
   */
  long waitingHandlerCount();

  /**
   * Checks if a waiting handler is registered (and therefore still waiting for a result) for the given unique id.
   *
   * @param queryUniqueId the unique id of the query to check for.
   * @return true if the handler is still registered, false otherwise.
   * @throws NullPointerException if the given unique id is null.
   */
  boolean hasWaitingHandler(@NonNull UUID queryUniqueId);

  /**
   * Gets and removes the waiting handler for the given query unique id.
   *
   * @param queryUniqueId the unique id of the handler to get.
   * @return the waiting handler associated with the given unique id, null if no handler with that id is waiting.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable
  CompletableFuture<Packet> waitingHandler(@NonNull UUID queryUniqueId);

  /**
   * Sends a query packet to the associated network channel, automatically selecting a query id for the packet and
   * setting it. Equivalent to {@code manager.sendQueryPacket(packet, UUID.randomUUID()}. An existing query unique id in
   * the packet will get overridden.
   *
   * @param packet the packet to convert to a query packet and send to the channel.
   * @return a future completed with either the response to the packet or an empty packet if the waiting time expires.
   * @throws NullPointerException if the given packet is null.
   */
  @NonNull
  CompletableFuture<Packet> sendQueryPacket(@NonNull Packet packet);
}
