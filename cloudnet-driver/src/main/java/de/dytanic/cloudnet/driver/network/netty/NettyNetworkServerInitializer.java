package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

final class NettyNetworkServerInitializer extends ChannelInitializer<Channel> {

    private final NettyNetworkServer nettyNetworkServer;

    private final HostAndPort hostAndPort;

    public NettyNetworkServerInitializer(NettyNetworkServer nettyNetworkServer, HostAndPort hostAndPort) {
        this.nettyNetworkServer = nettyNetworkServer;
        this.hostAndPort = hostAndPort;
    }

    @Override
    protected void initChannel(Channel ch) {
        if (nettyNetworkServer.sslContext != null) {
            ch.pipeline()
                    .addLast(nettyNetworkServer.sslContext.newHandler(ch.alloc()));
        }

        ch.pipeline()
                .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
                .addLast("packet-decoder", new NettyPacketDecoder())
                .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
                .addLast("packet-encoder", new NettyPacketEncoder())
                .addLast("network-server-handler", new NettyNetworkServerHandler(nettyNetworkServer, hostAndPort))
        ;
    }
}