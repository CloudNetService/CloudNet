package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

final class NettyNetworkClientInitializer extends ChannelInitializer<Channel> {

    private final NettyNetworkClient nettyNetworkClient;
    private final HostAndPort hostAndPort;

    private final Runnable handler;

    public NettyNetworkClientInitializer(NettyNetworkClient nettyNetworkClient, HostAndPort hostAndPort) {
        this(nettyNetworkClient, hostAndPort, null);
    }

    public NettyNetworkClientInitializer(NettyNetworkClient nettyNetworkClient, HostAndPort hostAndPort, Runnable handler) {
        this.nettyNetworkClient = nettyNetworkClient;
        this.hostAndPort = hostAndPort;
        this.handler = handler;
    }

    @Override
    protected void initChannel(Channel ch) {
        if (nettyNetworkClient.sslContext != null) {
            ch.pipeline()
                    .addLast(nettyNetworkClient.sslContext.newHandler(ch.alloc(), hostAndPort.getHost(), hostAndPort.getPort()));
        }

        ch.pipeline()
                .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
                .addLast("packet-decoder", new NettyPacketDecoder())
                .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
                .addLast("packet-encoder", new NettyPacketEncoder())
                .addLast("network-client-handler", new NettyNetworkClientHandler(nettyNetworkClient, hostAndPort));

        if (this.handler != null) {
            this.handler.run();
        }
    }
}