package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;

final class NettyHttpServerInitializer extends ChannelInitializer<Channel> {

    private final NettyHttpServer nettyHttpServer;
    private final HostAndPort hostAndPort;

    public NettyHttpServerInitializer(NettyHttpServer nettyHttpServer, HostAndPort hostAndPort) {
        this.nettyHttpServer = nettyHttpServer;
        this.hostAndPort = hostAndPort;
    }

    @Override
    protected void initChannel(@NotNull Channel ch) {
        if (this.nettyHttpServer.sslContext != null) {
            ch.pipeline()
                    .addLast(this.nettyHttpServer.sslContext.newHandler(ch.alloc()));
        }

        ch.pipeline()
                .addLast("http-request-decoder", new HttpRequestDecoder())
                .addLast("http-object-aggregator", new HttpObjectAggregator(Short.MAX_VALUE))
                .addLast("http-response-encoder", new HttpResponseEncoder())
                .addLast("http-chunk-handler", new ChunkedWriteHandler())
                .addLast("http-server-handler", new NettyHttpServerHandler(this.nettyHttpServer, this.hostAndPort))
        ;
    }
}