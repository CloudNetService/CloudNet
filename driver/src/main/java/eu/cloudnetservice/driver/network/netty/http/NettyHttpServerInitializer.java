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

package eu.cloudnetservice.driver.network.netty.http;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.netty.NettyOptionSettingChannelInitializer;
import io.netty5.channel.Channel;
import io.netty5.handler.codec.http.HttpContentCompressor;
import io.netty5.handler.codec.http.HttpObjectAggregator;
import io.netty5.handler.codec.http.HttpRequestDecoder;
import io.netty5.handler.codec.http.HttpResponseEncoder;
import io.netty5.handler.stream.ChunkedWriteHandler;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The default channel initializer used to initialize http server connections.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class NettyHttpServerInitializer extends NettyOptionSettingChannelInitializer {

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
  protected void doInitChannel(@NonNull Channel ch) {
    if (this.nettyHttpServer.sslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.nettyHttpServer.sslContext.newHandler(ch.bufferAllocator()));
    }

    ch.pipeline()
      .addLast("read-timeout-handler", new NettyIdleStateHandler(30))
      .addLast("http-request-decoder", new HttpRequestDecoder())
      .addLast("http-object-aggregator", new HttpObjectAggregator<>(Short.MAX_VALUE))
      .addLast("http-response-encoder", new HttpResponseEncoder())
      .addLast("http-response-compressor", new HttpContentCompressor())
      .addLast("http-chunk-handler", new ChunkedWriteHandler())
      .addLast("http-server-handler", new NettyHttpServerHandler(this.nettyHttpServer, this.hostAndPort));
  }
}
