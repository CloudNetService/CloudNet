package de.dytanic.cloudnet.driver.network.netty.server;

import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkChannel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

@ApiStatus.Internal
final class NettyNetworkServerHandler extends NettyNetworkHandler {

    private final NettyNetworkServer nettyNetworkServer;

    private final HostAndPort connectedAddress;

    public NettyNetworkServerHandler(NettyNetworkServer nettyNetworkServer, HostAndPort connectedAddress) {
        this.nettyNetworkServer = nettyNetworkServer;
        this.connectedAddress = connectedAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = new NettyNetworkChannel(ctx.channel(), this.nettyNetworkServer.getPacketRegistry(),
                this.nettyNetworkServer.networkChannelHandler.call(), this.connectedAddress, HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()), false);

        this.nettyNetworkServer.channels.add(this.channel);

        if (this.channel.getHandler() != null) {
            this.channel.getHandler().handleChannelInitialize(this.channel);
        }
    }

    @Override
    protected Collection<INetworkChannel> getChannels() {
        return this.nettyNetworkServer.channels;
    }

    @Override
    protected ITaskScheduler getTaskScheduler() {
        return this.nettyNetworkServer.taskScheduler;
    }
}