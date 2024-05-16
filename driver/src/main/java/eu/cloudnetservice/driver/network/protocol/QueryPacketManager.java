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

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the manager used by the cloud to manage incoming and outgoing query packets.
 *
 * @since 4.0
 */
public interface QueryPacketManager {

  /**
   * Get the duration before a query is marked as unanswered and the associated future gets completed with an empty
   * packet.
   *
   * @return the timeout of a query.
   */
  @NonNull Duration queryTimeout();

  /**
   * Get the network channel this manager is associated with. Each network channel has its own query manager.
   *
   * @return the network channel to which this manager belongs.
   * @see NetworkChannel#queryPacketManager()
   */
  @NonNull NetworkChannel networkChannel();

  /**
   * Get all registered queries which are currently waiting for a response.
   *
   * @return all waiting queries.
   */
  @NonNull
  @UnmodifiableView Map<UUID, Task<Packet>> waitingHandlers();

  /**
   * Checks if a waiting handler is registered (and therefore still waiting for a result) for the given unique id.
   *
   * @param queryUniqueId the unique id of the query to check for.
   * @return true if the handler is still registered, false otherwise.
   * @throws NullPointerException if the given unique id is null.
   */
  boolean hasWaitingHandler(@NonNull UUID queryUniqueId);

  /**
   * Unregisters the waiting handler by the given unique id.
   *
   * @param queryUniqueId the unique id of the handler to unregister.
   * @return true if the handler was unregistered, false otherwise.
   * @throws NullPointerException if the given handler unique id is null.
   */
  boolean unregisterWaitingHandler(@NonNull UUID queryUniqueId);

  /**
   * Gets, but does not remove the waiting handler for the given unique id. Null if no handler with the given unique id
   * is still registered.
   *
   * @param queryUniqueId the unique id of the handler to get.
   * @return the waiting handler associated with the given unique id, null if no handler with that id is waiting.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable Task<Packet> waitingHandler(@NonNull UUID queryUniqueId);

  /**
   * Sends a query packet to the associated network channel, automatically selecting a query id for the packet and
   * setting it. Equivalent to {@code manager.sendQueryPacket(packet, UUID.randomUUID()}. An existing query unique id in
   * the packet will get overridden.
   *
   * @param packet the packet to convert to a query packet and send to the channel.
   * @return a future completed with either the response to the packet or an empty packet if the waiting time expires.
   * @throws NullPointerException if the given packet is null.
   */
  @NonNull Task<Packet> sendQueryPacket(@NonNull Packet packet);

  /**
   * Sends a query packet to the associated network channel, automatically setting the id in the packet. An existing
   * query unique id in the packet will get overridden.
   *
   * @param packet        the packet to convert to a query packet and send to the channel.
   * @param queryUniqueId the unique id to use when sending the packet.
   * @return a future completed with either the response to the packet or an empty packet if the waiting time expires.
   * @throws NullPointerException if either the given packet or unique id is null.
   */
  @NonNull Task<Packet> sendQueryPacket(@NonNull Packet packet, @NonNull UUID queryUniqueId);
}
