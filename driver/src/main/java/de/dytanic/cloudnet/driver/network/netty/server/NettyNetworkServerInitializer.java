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

package de.dytanic.cloudnet.driver.network.netty.server;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketDecoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketEncoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.NonNull;

public class NettyNetworkServerInitializer extends ChannelInitializer<Channel> {

  private final HostAndPort serverLocalAddress;
  private final NettyNetworkServer networkServer;

  public NettyNetworkServerInitializer(NettyNetworkServer networkServer, HostAndPort serverLocalAddress) {
    this.networkServer = networkServer;
    this.serverLocalAddress = serverLocalAddress;
  }

  @Override
  protected void initChannel(@NonNull Channel ch) {
    if (this.networkServer.sslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.networkServer.sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
      .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
      .addLast("packet-encoder", new NettyPacketEncoder())
      .addLast("network-server-handler", new NettyNetworkServerHandler(this.networkServer, this.serverLocalAddress))
    ;
  }
}
