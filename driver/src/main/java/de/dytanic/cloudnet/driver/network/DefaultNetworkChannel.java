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

package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.QueryPacketManager;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultQueryPacketManager;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

public abstract class DefaultNetworkChannel implements INetworkChannel {

  private static final AtomicLong CHANNEL_ID_COUNTER = new AtomicLong();

  private final long channelId = CHANNEL_ID_COUNTER.addAndGet(1);

  private final QueryPacketManager queryPacketManager;
  private final IPacketListenerRegistry packetRegistry;

  private final HostAndPort serverAddress;
  private final HostAndPort clientAddress;

  private final boolean clientProvidedChannel;

  private INetworkChannelHandler handler;

  public DefaultNetworkChannel(
    IPacketListenerRegistry packetRegistry,
    HostAndPort serverAddress,
    HostAndPort clientAddress,
    boolean clientProvidedChannel,
    INetworkChannelHandler handler
  ) {
    this.queryPacketManager = new DefaultQueryPacketManager(this);
    this.packetRegistry = new DefaultPacketListenerRegistry(packetRegistry);
    this.serverAddress = serverAddress;
    this.clientAddress = clientAddress;
    this.clientProvidedChannel = clientProvidedChannel;
    this.handler = handler;
  }

  @Override
  public @NonNull ITask<IPacket> sendQueryAsync(@NonNull IPacket packet) {
    return this.queryPacketManager.sendQueryPacket(packet);
  }

  @Override
  public IPacket sendQuery(@NonNull IPacket packet) {
    return this.sendQueryAsync(packet).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public long channelId() {
    return this.channelId;
  }

  @Override
  public @NonNull IPacketListenerRegistry packetRegistry() {
    return this.packetRegistry;
  }

  @Override
  public @NonNull QueryPacketManager queryPacketManager() {
    return this.queryPacketManager;
  }

  @Override
  public @NonNull HostAndPort serverAddress() {
    return this.serverAddress;
  }

  @Override
  public @NonNull HostAndPort clientAddress() {
    return this.clientAddress;
  }

  @Override
  public boolean clientProvidedChannel() {
    return this.clientProvidedChannel;
  }

  @Override
  public @NonNull INetworkChannelHandler handler() {
    return this.handler;
  }

  @Override
  public void handler(@NonNull INetworkChannelHandler handler) {
    this.handler = handler;
  }

}
