package de.dytanic.cloudnet.driver.network.netty.client;

import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

@ApiStatus.Internal
final class NettyNetworkClientHandler extends NettyNetworkHandler {

    private final NettyNetworkClient nettyNetworkClient;

    private final HostAndPort connectedAddress;

    public NettyNetworkClientHandler(NettyNetworkClient nettyNetworkClient, HostAndPort connectedAddress) {
        this.nettyNetworkClient = nettyNetworkClient;
        this.connectedAddress = connectedAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channel = new NettyNetworkChannel(ctx.channel(), this.nettyNetworkClient.getPacketRegistry(),
                this.nettyNetworkClient.networkChannelHandler.call(), this.connectedAddress, HostAndPort.fromSocketAddress(ctx.channel().localAddress()), true);

        this.nettyNetworkClient.channels.add(super.channel);

        if (this.channel.getHandler() != null) {
            this.channel.getHandler().handleChannelInitialize(super.channel);
        }
    }

    @Override
    protected Collection<INetworkChannel> getChannels() {
        return this.nettyNetworkClient.channels;
    }

    @Override
    protected ITaskScheduler getTaskScheduler() {
        return this.nettyNetworkClient.taskScheduler;
    }
}