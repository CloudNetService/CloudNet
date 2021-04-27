package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

final class NettyNetworkServerHandler extends SimpleChannelInboundHandler<Packet> {

    private final HostAndPort connectedAddress;
    private final NettyNetworkServer nettyNetworkServer;

    private NettyNetworkChannel channel;

    public NettyNetworkServerHandler(NettyNetworkServer nettyNetworkServer, HostAndPort connectedAddress) {
        this.nettyNetworkServer = nettyNetworkServer;
        this.connectedAddress = connectedAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = new NettyNetworkChannel(
                ctx.channel(),
                this.nettyNetworkServer.getPacketRegistry(),
                this.nettyNetworkServer.networkChannelHandler.call(),
                this.connectedAddress,
                HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()),
                false
        );
        this.nettyNetworkServer.channels.add(this.channel);

        if (this.channel.getHandler() != null) {
            this.channel.getHandler().handleChannelInitialize(this.channel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
            if (this.channel.getHandler() != null) {
                this.channel.getHandler().handleChannelClose(this.channel);
            }

            ctx.channel().close();
            this.nettyNetworkServer.channels.remove(this.channel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
        try {
            if (this.channel.getHandler() == null || this.channel.getHandler().handlePacketReceive(this.channel, msg)) {
                this.channel.getPacketRegistry().handlePacket(this.channel, msg);
            }
        } catch (Exception exception) {
            CloudNetDriver.getInstance().getLogger().error("Exception whilst handling packet " + msg, exception);
        }
    }
}