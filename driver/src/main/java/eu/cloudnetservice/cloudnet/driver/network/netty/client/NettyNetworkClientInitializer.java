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

package eu.cloudnetservice.cloudnet.driver.network.netty.client;

import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketDecoder;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketEncoder;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketLengthSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.NonNull;

/**
 * The channel handler implementation responsible for initializing the newly opened channel.
 *
 * @since 4.0
 */
public class NettyNetworkClientInitializer extends ChannelInitializer<Channel> {

  protected final HostAndPort hostAndPort;
  protected final NettyNetworkClient nettyNetworkClient;

  /**
   * Constructs a new network client initializer instance.
   *
   * @param targetHost    the target host to which the network client connected.
   * @param networkClient the network client which connected to a server.
   * @throws NullPointerException if either the target host or client is null.
   */
  public NettyNetworkClientInitializer(@NonNull HostAndPort targetHost, @NonNull NettyNetworkClient networkClient) {
    this.hostAndPort = targetHost;
    this.nettyNetworkClient = networkClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initChannel(@NonNull Channel channel) {
    if (this.nettyNetworkClient.sslContext != null) {
      channel.pipeline().addLast("ssl-handler", this.nettyNetworkClient.sslContext.newHandler(
        channel.alloc(),
        this.hostAndPort.host(),
        this.hostAndPort.port()));
    }

    channel.pipeline()
      .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
      .addLast("packet-encoder", new NettyPacketEncoder())
      .addLast("network-client-handler", new NettyNetworkClientHandler(this.nettyNetworkClient, this.hostAndPort));
  }
}
