package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class NettyHttpServerInitializer extends ChannelInitializer<Channel> {

  private final NettyHttpServer nettyHttpServer;

  private final HostAndPort hostAndPort;

  @Override
  protected void initChannel(Channel ch) throws Exception {
    if (nettyHttpServer.sslContext != null) {
      ch.pipeline()
        .addLast(nettyHttpServer.sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
      .addLast("http-server-codec", new HttpServerCodec())
      .addLast("http-object-aggregator",
        new HttpObjectAggregator(Short.MAX_VALUE))
      .addLast("http-server-handler",
        new NettyHttpServerHandler(nettyHttpServer, hostAndPort))
    ;
  }
}