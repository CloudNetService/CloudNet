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

package eu.cloudnetservice.node.impl.cluster.util;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.QueryPacketManager;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;

public final class QueuedNetworkChannel implements NetworkChannel {

  private final NetworkChannel wrappedChannel;
  private final Queue<Packet> scheduledPackets;

  public QueuedNetworkChannel(@NonNull NetworkChannel wrappedChannel) {
    this.wrappedChannel = wrappedChannel;
    this.scheduledPackets = new ConcurrentLinkedQueue<>();
  }

  @Override
  public long channelId() {
    return this.wrappedChannel.channelId();
  }

  @Override
  public @NonNull HostAndPort serverAddress() {
    return this.wrappedChannel.serverAddress();
  }

  @Override
  public @NonNull HostAndPort clientAddress() {
    return this.wrappedChannel.clientAddress();
  }

  @Override
  public @NonNull NetworkChannelHandler handler() {
    return this.wrappedChannel.handler();
  }

  @Override
  public @NonNull PacketListenerRegistry packetRegistry() {
    return this.wrappedChannel.packetRegistry();
  }

  @Override
  public @NonNull QueryPacketManager queryPacketManager() {
    return this.wrappedChannel.queryPacketManager();
  }

  @Override
  public boolean clientProvidedChannel() {
    return this.wrappedChannel.clientProvidedChannel();
  }

  @Override
  public @NonNull CompletableFuture<Packet> sendQueryAsync(@NonNull Packet packet) {
    return this.wrappedChannel.sendQueryAsync(packet);
  }

  @Override
  public boolean writeable() {
    return true;
  }

  @Override
  public boolean active() {
    return false;
  }

  @Override
  public void close() {
    this.wrappedChannel.close();
    this.scheduledPackets.clear();
  }

  @Override
  public void sendPacket(@NonNull Packet packet) {
    this.scheduledPackets.add(packet);
  }

  @Override
  public void sendPacketSync(@NonNull Packet packet) {
    this.scheduledPackets.add(packet);
  }

  public void drainPacketQueue(@NonNull NetworkChannel target) {
    this.scheduledPackets.forEach(target::sendPacketSync);
    this.scheduledPackets.clear();
  }
}
