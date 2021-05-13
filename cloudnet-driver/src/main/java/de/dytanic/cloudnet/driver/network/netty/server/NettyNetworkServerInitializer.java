package de.dytanic.cloudnet.driver.network.netty.server;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketDecoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketEncoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyNetworkServerInitializer extends ChannelInitializer<Channel> {

    private final NettyNetworkServer nettyNetworkServer;

    private final HostAndPort hostAndPort;

    public NettyNetworkServerInitializer(NettyNetworkServer nettyNetworkServer, HostAndPort hostAndPort) {
        this.nettyNetworkServer = nettyNetworkServer;
        this.hostAndPort = hostAndPort;
    }

    @Override
    protected void initChannel(Channel ch) {
        if (this.nettyNetworkServer.sslContext != null) {
            ch.pipeline()
                    .addLast(this.nettyNetworkServer.sslContext.newHandler(ch.alloc()));
        }

        ch.pipeline()
                .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
                .addLast("packet-decoder", new NettyPacketDecoder())
                .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
                .addLast("packet-encoder", new NettyPacketEncoder())
                .addLast("network-server-handler", new NettyNetworkServerHandler(this.nettyNetworkServer, this.hostAndPort))
        ;
    }
}