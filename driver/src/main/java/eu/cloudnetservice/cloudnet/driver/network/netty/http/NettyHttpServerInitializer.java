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

package eu.cloudnetservice.cloudnet.driver.network.netty.http;

import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * The default channel initializer used to initialize http server connections.
 *
 * @since 4.0
 */
@Internal
final class NettyHttpServerInitializer extends ChannelInitializer<Channel> {

  private final NettyHttpServer nettyHttpServer;
  private final HostAndPort hostAndPort;

  /**
   * Constructs a new netty http server initializer instance.
   *
   * @param nettyHttpServer the http server the initializer belongs to.
   * @param hostAndPort     the host and port of the listener which was bound.
   * @throws NullPointerException if either the http server or host and port is null.
   */
  public NettyHttpServerInitializer(@NonNull NettyHttpServer nettyHttpServer, @NonNull HostAndPort hostAndPort) {
    this.nettyHttpServer = nettyHttpServer;
    this.hostAndPort = hostAndPort;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initChannel(@NonNull Channel ch) {
    if (this.nettyHttpServer.sslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.nettyHttpServer.sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
      .addLast("http-request-decoder", new HttpRequestDecoder())
      .addLast("http-object-aggregator", new HttpObjectAggregator(Short.MAX_VALUE))
      .addLast("http-response-encoder", new HttpResponseEncoder())
      .addLast("http-chunk-handler", new ChunkedWriteHandler())
      .addLast("http-server-handler", new NettyHttpServerHandler(this.nettyHttpServer, this.hostAndPort));
  }
}
