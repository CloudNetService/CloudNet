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

package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketSendEvent;
import de.dytanic.cloudnet.driver.network.DefaultNetworkChannel;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public final class NettyNetworkChannel extends DefaultNetworkChannel implements INetworkChannel {

  private static final Logger LOGGER = LogManager.getLogger(NettyNetworkChannel.class);

  private final Channel channel;

  public NettyNetworkChannel(Channel channel, IPacketListenerRegistry packetRegistry, INetworkChannelHandler handler,
    HostAndPort serverAddress, HostAndPort clientAddress, boolean clientProvidedChannel) {
    super(packetRegistry, serverAddress, clientAddress, clientProvidedChannel, handler);
    this.channel = channel;
  }

  @Override
  public void sendPacket(@NotNull IPacket... packets) {
    for (var packet : packets) {
      this.writePacket(packet, false);
    }
    this.channel.flush(); // reduces i/o load
  }

  @Override
  public void sendPacketSync(@NotNull IPacket... packets) {
    for (var packet : packets) {
      var future = this.writePacket(packet, false);
      if (future != null) {
        future.syncUninterruptibly();
      }
    }
    this.channel.flush(); // reduces i/o load
  }

  @Override
  public void sendPacket(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    if (this.channel.eventLoop().inEventLoop()) {
      this.writePacket(packet, true);
    } else {
      this.channel.eventLoop().execute(() -> this.writePacket(packet, true));
    }
  }

  @Override
  public void sendPacketSync(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    var future = this.writePacket(packet, true);
    if (future != null) {
      future.syncUninterruptibly();
    }
  }

  @Override
  public boolean isWriteable() {
    return this.channel.isWritable();
  }

  @Override
  public boolean isActive() {
    return this.channel.isActive();
  }

  private @Nullable ChannelFuture writePacket(IPacket packet, boolean flushAfter) {
    if (!CloudNetDriver.getInstance().getEventManager().callEvent(
      new NetworkChannelPacketSendEvent(this, packet)
    ).isCancelled()) {
      return flushAfter ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
    } else {
      return null;
    }
  }

  @Override
  public void close() {
    this.channel.close();
  }

  public Channel getChannel() {
    return this.channel;
  }

}
