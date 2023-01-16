/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.QueryPacketManager;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of the query manager.
 *
 * @since 4.0
 */
public class DefaultQueryPacketManager implements QueryPacketManager {

  private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(30);

  private final Duration queryTimeout;
  private final NetworkChannel networkChannel;
  private final Cache<UUID, Task<Packet>> waitingHandlers;

  /**
   * Constructs a new query manager for the given network channel and a timeout of 30 seconds for each query.
   *
   * @param networkChannel the network channel associated with this manager.
   * @throws NullPointerException if the given network channel is null.
   */
  public DefaultQueryPacketManager(@NonNull NetworkChannel networkChannel) {
    this(networkChannel, DEFAULT_TIMEOUT_DURATION);
  }

  /**
   * Constructs a new query manager for the given network with the provided query timeout.
   *
   * @param networkChannel the network channel associated with this manager.
   * @param queryTimeout   the time to wait for a response to each query before being completed with an empty packet.
   * @throws NullPointerException if either the given network channel or query timeout is null.
   */
  public DefaultQueryPacketManager(@NonNull NetworkChannel networkChannel, @NonNull Duration queryTimeout) {
    this.networkChannel = networkChannel;
    this.queryTimeout = queryTimeout;
    // construct the cache based on the given information
    this.waitingHandlers = Caffeine.newBuilder()
      .expireAfterWrite(queryTimeout)
      .scheduler(Scheduler.systemScheduler())
      .removalListener(this.newRemovalListener())
      .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Duration queryTimeout() {
    return this.queryTimeout;
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
  public @NonNull @UnmodifiableView Map<UUID, Task<Packet>> waitingHandlers() {
    return Collections.unmodifiableMap(this.waitingHandlers.asMap());
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
  public boolean unregisterWaitingHandler(@NonNull UUID queryUniqueId) {
    this.waitingHandlers.invalidate(queryUniqueId);
    return true;
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
    return this.sendQueryPacket(packet, UUID.randomUUID());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Packet> sendQueryPacket(@NonNull Packet packet, @NonNull UUID queryUniqueId) {
    // create & register the result handler
    var task = new Task<Packet>();
    this.waitingHandlers.put(queryUniqueId, task);
    // set the unique id of the packet and send
    packet.uniqueId(queryUniqueId);
    this.networkChannel.sendPacketSync(packet);
    // return the created handler
    return task;
  }

  /**
   * Constructs a new removal listener for the cache, completing the future of a query packet with a timeout exception
   * when evicted from the cache.
   *
   * @return a new removal listeners for unanswered packet future completion.
   */
  protected @NonNull RemovalListener<UUID, Task<Packet>> newRemovalListener() {
    return ($, value, cause) -> {
      if (cause.wasEvicted() && value != null) {
        value.completeExceptionally(new TimeoutException());
      }
    };
  }
}
