/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.netty.client;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.netty.NettyOptionSettingChannelInitializer;
import eu.cloudnetservice.driver.network.netty.codec.NettyPacketDecoder;
import eu.cloudnetservice.driver.network.netty.codec.NettyPacketEncoder;
import eu.cloudnetservice.driver.network.netty.codec.VarInt32FrameDecoder;
import eu.cloudnetservice.driver.network.netty.codec.VarInt32FramePrepender;
import io.netty5.channel.Channel;
import lombok.NonNull;

/**
 * The channel handler implementation responsible for initializing the newly opened channel.
 *
 * @since 4.0
 */
public class NettyNetworkClientInitializer extends NettyOptionSettingChannelInitializer {

  protected final HostAndPort hostAndPort;
  protected final EventManager eventManager;
  protected final NettyNetworkClient nettyNetworkClient;

  /**
   * Constructs a new network client initializer instance.
   *
   * @param targetHost    the target host to which the network client connected.
   * @param eventManager  the event manager of the current component.
   * @param networkClient the network client which connected to a server.
   * @throws NullPointerException if either the event manager, target host or client is null.
   */
  public NettyNetworkClientInitializer(
    @NonNull HostAndPort targetHost,
    @NonNull EventManager eventManager,
    @NonNull NettyNetworkClient networkClient
  ) {
    this.hostAndPort = targetHost;
    this.eventManager = eventManager;
    this.nettyNetworkClient = networkClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doInitChannel(@NonNull Channel channel) {
    if (this.nettyNetworkClient.sslContext != null) {
      channel.pipeline().addLast("ssl-handler", this.nettyNetworkClient.sslContext.newHandler(
        channel.bufferAllocator(),
        this.hostAndPort.host(),
        this.hostAndPort.port()));
    }

    channel.pipeline()
      .addLast("packet-length-deserializer", new VarInt32FrameDecoder())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", VarInt32FramePrepender.INSTANCE)
      .addLast("packet-encoder", NettyPacketEncoder.INSTANCE)
      .addLast("network-client-handler",
        new NettyNetworkClientHandler(this.eventManager, this.nettyNetworkClient, this.hostAndPort));
  }
}
