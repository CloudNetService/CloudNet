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
import de.dytanic.cloudnet.common.logging.LogLevel;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class NettyNetworkChannel extends DefaultNetworkChannel implements INetworkChannel {

  private final Channel channel;

  public NettyNetworkChannel(Channel channel, IPacketListenerRegistry packetRegistry, INetworkChannelHandler handler,
    HostAndPort serverAddress, HostAndPort clientAddress, boolean clientProvidedChannel) {
    super(packetRegistry, serverAddress, clientAddress, clientProvidedChannel, handler);
    this.channel = channel;
  }

  @Override
  public void sendPacket(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    if (this.channel.eventLoop().inEventLoop()) {
      this.writePacket(packet);
    } else {
      this.channel.eventLoop().execute(() -> this.writePacket(packet));
    }
  }

  @Override
  public void sendPacketSync(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    ChannelFuture future = this.writePacket(packet);
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

  private ChannelFuture writePacket(IPacket packet) {
    NetworkChannelPacketSendEvent event = new NetworkChannelPacketSendEvent(this, packet);

    CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> cloudNetDriver.getEventManager().callEvent(event));

    if (!event.isCancelled()) {
      if (packet.isShowDebug()) {
        CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
          if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
            cloudNetDriver.getLogger().debug(
              String.format(
                "Sending packet to %s on channel %d with id %s, header=%s;body=%d",
                this.getClientAddress().toString(),
                packet.getChannel(),
                packet.getUniqueId(),
                packet.getHeader().toJson(),
                packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
              )
            );
          }
        });
      }

      return this.channel.writeAndFlush(packet);
    }

    return null;
  }

  @Override
  public void close() {
    this.channel.close();
  }

  public Channel getChannel() {
    return this.channel;
  }

}
