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

package eu.cloudnetservice.driver.impl.network.netty.server;

import eu.cloudnetservice.driver.impl.network.netty.NettyNetworkChannel;
import eu.cloudnetservice.driver.impl.network.netty.NettyNetworkHandler;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import io.netty5.channel.ChannelHandlerContext;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;

/**
 * The default netty based implementation of the netty network handler responsible to provide basic access to the
 * network server handling.
 *
 * @since 4.0
 */
public class NettyNetworkServerHandler extends NettyNetworkHandler {

  private final HostAndPort serverLocalAddress;
  private final NettyNetworkServer networkServer;

  /**
   * Constructs a new network server handler instance.
   *
   * @param networkServer      the network server associated with this handler.
   * @param serverLocalAddress the server address this handler is associated with.
   * @throws NullPointerException if either the given event manager, server or address is null.
   */
  public NettyNetworkServerHandler(@NonNull NettyNetworkServer networkServer, @NonNull HostAndPort serverLocalAddress) {
    this.networkServer = networkServer;
    this.serverLocalAddress = serverLocalAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelActive(@NonNull ChannelHandlerContext ctx) throws Exception {
    this.networkServer.channels.add(this.channel = new NettyNetworkChannel(
      ctx.channel(),
      this.networkServer.packetRegistry(),
      this.networkServer.handlerFactory.call(),
      this.serverLocalAddress,
      HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()),
      false
    ));
    this.channel.handler().handleChannelInitialize(this.channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Collection<NetworkChannel> channels() {
    return this.networkServer.channels;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Executor packetDispatcher() {
    return this.networkServer.packetDispatcher();
  }
}
