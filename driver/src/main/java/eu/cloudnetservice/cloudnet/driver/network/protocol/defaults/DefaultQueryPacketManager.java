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

package eu.cloudnetservice.cloudnet.driver.network.protocol.defaults;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.protocol.BasePacket;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.protocol.QueryPacketManager;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultQueryPacketManager implements QueryPacketManager {

  private final long queryTimeoutMillis;
  private final NetworkChannel networkChannel;
  private final Cache<UUID, CompletableTask<Packet>> waitingHandlers;

  public DefaultQueryPacketManager(NetworkChannel networkChannel) {
    this(networkChannel, TimeUnit.SECONDS.toMillis(30));
  }

  public DefaultQueryPacketManager(NetworkChannel networkChannel, long queryTimeoutMillis) {
    this.networkChannel = networkChannel;
    this.queryTimeoutMillis = queryTimeoutMillis;
    this.waitingHandlers = CacheBuilder.newBuilder()
      .removalListener(this.newRemovalListener())
      .expireAfterWrite(queryTimeoutMillis, TimeUnit.MILLISECONDS)
      .build();
  }

  @Override
  public long queryTimeoutMillis() {
    return this.queryTimeoutMillis;
  }

  @Override
  public @NonNull NetworkChannel networkChannel() {
    return this.networkChannel;
  }

  @Override
  public @NonNull @UnmodifiableView Map<UUID, CompletableTask<Packet>> waitingHandlers() {
    return Collections.unmodifiableMap(this.waitingHandlers.asMap());
  }

  @Override
  public boolean hasWaitingHandler(@NonNull UUID queryUniqueId) {
    return this.waitingHandlers.getIfPresent(queryUniqueId) != null;
  }

  @Override
  public boolean unregisterWaitingHandler(@NonNull UUID queryUniqueId) {
    this.waitingHandlers.invalidate(queryUniqueId);
    return true;
  }

  @Override
  public @Nullable CompletableTask<Packet> waitingHandler(@NonNull UUID queryUniqueId) {
    var task = this.waitingHandlers.getIfPresent(queryUniqueId);
    if (task != null) {
      this.waitingHandlers.invalidate(queryUniqueId);
    }
    return task;
  }

  @Override
  public @NonNull CompletableTask<Packet> sendQueryPacket(@NonNull Packet packet) {
    return this.sendQueryPacket(packet, UUID.randomUUID());
  }

  @Override
  public @NonNull CompletableTask<Packet> sendQueryPacket(@NonNull Packet packet, @NonNull UUID queryUniqueId) {
    // create & register the result handler
    var task = new CompletableTask<Packet>();
    this.waitingHandlers.put(queryUniqueId, task);
    // set the unique id of the packet and send
    packet.uniqueId(queryUniqueId);
    this.networkChannel.sendPacket(packet);
    // return the created handler
    return task;
  }

  protected @NonNull RemovalListener<UUID, CompletableTask<Packet>> newRemovalListener() {
    return notification -> {
      if (notification.wasEvicted() && notification.getValue() != null) {
        notification.getValue().complete(BasePacket.EMPTY);
      }
    };
  }
}
