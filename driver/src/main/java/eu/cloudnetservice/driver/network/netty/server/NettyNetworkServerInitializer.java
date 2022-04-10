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

package eu.cloudnetservice.driver.network.netty.server;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.netty.codec.NettyPacketDecoder;
import eu.cloudnetservice.driver.network.netty.codec.NettyPacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.NonNull;

/**
 * The default netty based handler responsible to initialize new channels which get connected to the current node.
 *
 * @since 4.0
 */
public class NettyNetworkServerInitializer extends ChannelInitializer<Channel> {

  private final HostAndPort serverLocalAddress;
  private final NettyNetworkServer networkServer;

  /**
   * Constructs a new network initializer instance.
   *
   * @param networkServer      the network server this handler belongs to.
   * @param serverLocalAddress the local address associated with this handler.
   * @throws NullPointerException if then given server or address is null.
   */
  public NettyNetworkServerInitializer(
    @NonNull NettyNetworkServer networkServer,
    @NonNull HostAndPort serverLocalAddress
  ) {
    this.networkServer = networkServer;
    this.serverLocalAddress = serverLocalAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initChannel(@NonNull Channel ch) {
    if (this.networkServer.sslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.networkServer.sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
      .addLast("packet-length-deserializer", new ProtobufVarint32FrameDecoder())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", new ProtobufVarint32LengthFieldPrepender())
      .addLast("packet-encoder", new NettyPacketEncoder())
      .addLast("network-server-handler", new NettyNetworkServerHandler(this.networkServer, this.serverLocalAddress));
  }
}
