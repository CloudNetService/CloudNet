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