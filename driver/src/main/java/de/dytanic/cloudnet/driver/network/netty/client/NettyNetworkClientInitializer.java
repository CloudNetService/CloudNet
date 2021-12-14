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

package de.dytanic.cloudnet.driver.network.netty.client;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketDecoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketEncoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.NotNull;

public class NettyNetworkClientInitializer extends ChannelInitializer<Channel> {

  protected final HostAndPort hostAndPort;
  protected final NettyNetworkClient nettyNetworkClient;

  public NettyNetworkClientInitializer(HostAndPort targetHost, NettyNetworkClient networkClient) {
    this.hostAndPort = targetHost;
    this.nettyNetworkClient = networkClient;
  }

  @Override
  protected void initChannel(@NotNull Channel channel) {
    if (this.nettyNetworkClient.sslContext != null) {
      var handler = this.nettyNetworkClient.sslContext.newHandler(
        channel.alloc(),
        this.hostAndPort.getHost(),
        this.hostAndPort.getPort());
      channel.pipeline().addLast("ssl-handler", handler);
    }

    channel.pipeline()
      .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
      .addLast("packet-encoder", new NettyPacketEncoder())
      .addLast("network-client-handler", new NettyNetworkClientHandler(this.nettyNetworkClient, this.hostAndPort));
  }
}
