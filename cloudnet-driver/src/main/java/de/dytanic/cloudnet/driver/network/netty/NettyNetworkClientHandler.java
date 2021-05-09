package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.util.concurrent.Executor;

final class NettyNetworkClientHandler extends SimpleChannelInboundHandler<Packet> {

    private final HostAndPort connectedAddress;
    private final NettyNetworkClient nettyNetworkClient;
    private final Executor packetDispatcher = NettyUtils.newPacketDispatcher();

    private NettyNetworkChannel channel;

    public NettyNetworkClientHandler(NettyNetworkClient nettyNetworkClient, HostAndPort connectedAddress) {
        this.nettyNetworkClient = nettyNetworkClient;
        this.connectedAddress = connectedAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = new NettyNetworkChannel(
                ctx.channel(),
                this.nettyNetworkClient.getPacketRegistry(),
                this.nettyNetworkClient.networkChannelHandler.call(),
                this.connectedAddress,
                HostAndPort.fromSocketAddress(ctx.channel().localAddress()),
                true
        );
        this.nettyNetworkClient.channels.add(this.channel);

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
            this.nettyNetworkClient.channels.remove(this.channel);
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
        this.packetDispatcher.execute(() -> {
            try {
                if (this.channel.getHandler() == null || this.channel.getHandler().handlePacketReceive(this.channel, msg)) {
                    this.channel.getPacketRegistry().handlePacket(this.channel, msg);
                }
            } catch (Exception exception) {
                CloudNetDriver.getInstance().getLogger().error("Exception whilst handling packet " + msg, exception);
            }
        });
    }
}