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

package eu.cloudnetservice.driver.impl.network.netty.client;

import eu.cloudnetservice.driver.impl.network.netty.NettyNetworkChannel;
import eu.cloudnetservice.driver.impl.network.netty.NettyNetworkHandler;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import io.netty5.channel.ChannelHandlerContext;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;

/**
 * Constructs a default implementation for a client network handler, delegating most of the calls to the network
 * client.
 *
 * @since 4.0
 */
public class NettyNetworkClientHandler extends NettyNetworkHandler {

  private final HostAndPort connectedAddress;
  private final NettyNetworkClient nettyNetworkClient;

  /**
   * Constructs a new network client handler instance.
   *
   * @param nettyNetworkClient the client which connected to the endpoint.
   * @param connectedAddress   the server address to which the client connected.
   * @throws NullPointerException if either the given event manager, client or connect address is null.
   */
  public NettyNetworkClientHandler(
    @NonNull NettyNetworkClient nettyNetworkClient,
    @NonNull HostAndPort connectedAddress
  ) {
    this.nettyNetworkClient = nettyNetworkClient;
    this.connectedAddress = connectedAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelActive(@NonNull ChannelHandlerContext ctx) throws Exception {
    super.channel = new NettyNetworkChannel(
      ctx.channel(),
      this.nettyNetworkClient.packetRegistry(),
      this.nettyNetworkClient.handlerFactory.call(),
      this.connectedAddress,
      HostAndPort.fromSocketAddress(ctx.channel().localAddress()),
      true);
    this.nettyNetworkClient.channels.add(super.channel);
    // post the channel initialize to the handler
    this.channel.handler().handleChannelInitialize(super.channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Collection<NetworkChannel> channels() {
    return this.nettyNetworkClient.channels;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Executor packetDispatcher() {
    return this.nettyNetworkClient.packetDispatcher();
  }
}
