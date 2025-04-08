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

import eu.cloudnetservice.driver.impl.network.netty.NettyOptionSettingChannelInitializer;
import eu.cloudnetservice.driver.impl.network.netty.codec.NettyPacketDecoder;
import eu.cloudnetservice.driver.impl.network.netty.codec.NettyPacketEncoder;
import eu.cloudnetservice.driver.impl.network.netty.codec.VarInt32FrameDecoder;
import eu.cloudnetservice.driver.impl.network.netty.codec.VarInt32FramePrepender;
import eu.cloudnetservice.driver.network.HostAndPort;
import io.netty5.channel.Channel;
import lombok.NonNull;

/**
 * The default netty based handler responsible to initialize new channels which get connected to the current node.
 *
 * @since 4.0
 */
public class NettyNetworkServerInitializer extends NettyOptionSettingChannelInitializer {

  private final HostAndPort serverLocalAddress;
  private final NettyNetworkServer networkServer;

  /**
   * Constructs a new network initializer instance.
   *
   * @param networkServer      the network server this handler belongs to.
   * @param serverLocalAddress the local address associated with this handler.
   * @throws NullPointerException if the given event manager, server or address is null.
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
  protected void doInitChannel(@NonNull Channel ch) {
    if (this.networkServer.sslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.networkServer.sslContext.newHandler(ch.bufferAllocator()));
    }

    ch.pipeline()
      .addLast("packet-length-deserializer", new VarInt32FrameDecoder())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", VarInt32FramePrepender.INSTANCE)
      .addLast("packet-encoder", NettyPacketEncoder.INSTANCE)
      .addLast("network-server-handler", new NettyNetworkServerHandler(this.networkServer, this.serverLocalAddress));
  }
}
