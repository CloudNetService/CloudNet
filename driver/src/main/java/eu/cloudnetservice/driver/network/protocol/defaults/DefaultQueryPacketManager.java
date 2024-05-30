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

package eu.cloudnetservice.driver.network.protocol.defaults;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.QueryPacketManager;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the query manager.
 *
 * @since 4.0
 */
public class DefaultQueryPacketManager implements QueryPacketManager {

  protected final NetworkChannel networkChannel;
  protected final Cache<UUID, Task<Packet>> waitingHandlers;

  /**
   * Constructs a new query manager for the given network with the provided query timeout.
   *
   * @param networkChannel the network channel associated with this manager.
   * @throws NullPointerException if the given network channel is null.
   */
  public DefaultQueryPacketManager(@NonNull NetworkChannel networkChannel) {
    this.networkChannel = networkChannel;
    this.waitingHandlers = Caffeine.newBuilder()
      .weakValues()
      .removalListener(this.newRemovalListener())
      .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull NetworkChannel networkChannel() {
    return this.networkChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long waitingHandlerCount() {
    return this.waitingHandlers.estimatedSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasWaitingHandler(@NonNull UUID queryUniqueId) {
    return this.waitingHandlers.getIfPresent(queryUniqueId) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Task<Packet> waitingHandler(@NonNull UUID queryUniqueId) {
    var task = this.waitingHandlers.getIfPresent(queryUniqueId);
    if (task != null) {
      this.waitingHandlers.invalidate(queryUniqueId);
    }
    return task;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Packet> sendQueryPacket(@NonNull Packet packet) {
    // constructs and register a task for the given query unique id. note that if a replacement by key is
    // happening in the cache, the eviction listener is called and the previous future is automatically
    // cancelled, therefore there is no need to explicitly check for that here.
    var responseTask = new Task<Packet>();
    var queryUniqueId = UUID.randomUUID();
    this.waitingHandlers.put(queryUniqueId, responseTask);

    packet.uniqueId(queryUniqueId);
    this.networkChannel.sendPacketSync(packet);
    return responseTask;
  }

  /**
   * Constructs a new removal listener for the cache, completing the future of a query packet with a timeout exception
   * when evicted from the cache.
   *
   * @return a new removal listeners for unanswered packet future completion.
   */
  protected @NonNull RemovalListener<UUID, Task<Packet>> newRemovalListener() {
    return (_, value, cause) -> {
      if (cause != RemovalCause.EXPLICIT && value != null) {
        value.completeExceptionally(new TimeoutException());
      }
    };
  }
}
