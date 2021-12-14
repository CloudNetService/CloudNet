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

package de.dytanic.cloudnet.driver.network.protocol.defaults;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.QueryPacketManager;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultQueryPacketManager implements QueryPacketManager {

  private final long queryTimeoutMillis;
  private final INetworkChannel networkChannel;
  private final Cache<UUID, CompletableTask<IPacket>> waitingHandlers;

  public DefaultQueryPacketManager(INetworkChannel networkChannel) {
    this(networkChannel, TimeUnit.SECONDS.toMillis(30));
  }

  public DefaultQueryPacketManager(INetworkChannel networkChannel, long queryTimeoutMillis) {
    this.networkChannel = networkChannel;
    this.queryTimeoutMillis = queryTimeoutMillis;
    this.waitingHandlers = CacheBuilder.newBuilder()
      .removalListener(this.newRemovalListener())
      .expireAfterWrite(queryTimeoutMillis, TimeUnit.MILLISECONDS)
      .build();
  }

  @Override
  public long getQueryTimeoutMillis() {
    return this.queryTimeoutMillis;
  }

  @Override
  public @NotNull INetworkChannel getNetworkChannel() {
    return this.networkChannel;
  }

  @Override
  public @NotNull @UnmodifiableView Map<UUID, CompletableTask<IPacket>> getWaitingHandlers() {
    return Collections.unmodifiableMap(this.waitingHandlers.asMap());
  }

  @Override
  public boolean hasWaitingHandler(@NotNull UUID queryUniqueId) {
    return this.waitingHandlers.getIfPresent(queryUniqueId) != null;
  }

  @Override
  public boolean unregisterWaitingHandler(@NotNull UUID queryUniqueId) {
    this.waitingHandlers.invalidate(queryUniqueId);
    return true;
  }

  @Override
  public @Nullable CompletableTask<IPacket> getWaitingHandler(@NotNull UUID queryUniqueId) {
    var task = this.waitingHandlers.getIfPresent(queryUniqueId);
    if (task != null) {
      this.waitingHandlers.invalidate(queryUniqueId);
    }
    return task;
  }

  @Override
  public @NotNull CompletableTask<IPacket> sendQueryPacket(@NotNull IPacket packet) {
    return this.sendQueryPacket(packet, UUID.randomUUID());
  }

  @Override
  public @NotNull CompletableTask<IPacket> sendQueryPacket(@NotNull IPacket packet, @NotNull UUID queryUniqueId) {
    // create & register the result handler
    var task = new CompletableTask<IPacket>();
    this.waitingHandlers.put(queryUniqueId, task);
    // set the unique id of the packet and send
    packet.setUniqueId(queryUniqueId);
    this.networkChannel.sendPacket(packet);
    // return the created handler
    return task;
  }

  protected @NotNull RemovalListener<UUID, CompletableTask<IPacket>> newRemovalListener() {
    return notification -> {
      if (notification.wasEvicted()) {
        notification.getValue().complete(Packet.EMPTY);
      }
    };
  }
}
