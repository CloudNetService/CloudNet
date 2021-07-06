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

package de.dytanic.cloudnet.driver.network.netty.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyHttpServerInitializer extends ChannelInitializer<Channel> {

  private final NettyHttpServer nettyHttpServer;

  private final HostAndPort hostAndPort;

  public NettyHttpServerInitializer(NettyHttpServer nettyHttpServer, HostAndPort hostAndPort) {
    this.nettyHttpServer = nettyHttpServer;
    this.hostAndPort = hostAndPort;
  }

  @Override
  protected void initChannel(Channel ch) {
    if (this.nettyHttpServer.sslContext != null) {
      ch.pipeline()
        .addLast(this.nettyHttpServer.sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
      .addLast("http-server-codec", new HttpServerCodec())
      .addLast("http-object-aggregator", new HttpObjectAggregator(Short.MAX_VALUE))
      .addLast("http-server-handler", new NettyHttpServerHandler(this.nettyHttpServer, this.hostAndPort))
    ;
  }
}
