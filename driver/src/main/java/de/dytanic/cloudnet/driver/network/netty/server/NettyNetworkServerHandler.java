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
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NettyNetworkServerHandler extends NettyNetworkHandler {

  private final HostAndPort serverLocalAddress;
  private final NettyNetworkServer networkServer;

  public NettyNetworkServerHandler(NettyNetworkServer networkServer, HostAndPort serverLocalAddress) {
    this.networkServer = networkServer;
    this.serverLocalAddress = serverLocalAddress;
  }

  @Override
  public void channelActive(@NonNull ChannelHandlerContext ctx) throws Exception {
    this.networkServer.channels.add(this.channel = new NettyNetworkChannel(
      ctx.channel(),
      this.networkServer.packetRegistry(),
      this.networkServer.networkChannelHandlerFactory.call(),
      this.serverLocalAddress,
      HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()),
      false
    ));

    if (this.channel.handler() != null) {
      this.channel.handler().handleChannelInitialize(this.channel);
    }
  }

  @Override
  protected @NonNull Collection<NetworkChannel> channels() {
    return this.networkServer.channels;
  }

  @Override
  protected @NonNull Executor packetDispatcher() {
    return this.networkServer.packetDispatcher();
  }
}
