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

package de.dytanic.cloudnet.driver.network.netty.server;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Collection;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyNetworkServerHandler extends NettyNetworkHandler {

  private final HostAndPort connectedAddress;
  private final NettyNetworkServer nettyNetworkServer;

  public NettyNetworkServerHandler(NettyNetworkServer nettyNetworkServer, HostAndPort connectedAddress) {
    this.nettyNetworkServer = nettyNetworkServer;
    this.connectedAddress = connectedAddress;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.channel = new NettyNetworkChannel(
      ctx.channel(),
      this.nettyNetworkServer.getPacketRegistry(),
      this.nettyNetworkServer.networkChannelHandler.call(),
      this.connectedAddress,
      HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()),
      false
    );
    this.nettyNetworkServer.channels.add(this.channel);

    if (this.channel.getHandler() != null) {
      this.channel.getHandler().handleChannelInitialize(this.channel);
    }
  }

  @Override
  protected Collection<INetworkChannel> getChannels() {
    return this.nettyNetworkServer.channels;
  }

  @Override
  protected Executor getPacketDispatcher() {
    return this.nettyNetworkServer.getPacketDispatcher();
  }
}
