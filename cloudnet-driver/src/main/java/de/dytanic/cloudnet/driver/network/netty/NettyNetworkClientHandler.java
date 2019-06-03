package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
final class NettyNetworkClientHandler extends SimpleChannelInboundHandler<Packet> {

    private final NettyNetworkClient nettyNetworkClient;

    private final HostAndPort connectedAddress;

    private NettyNetworkChannel channel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = new NettyNetworkChannel(ctx.channel(), this.nettyNetworkClient.getPacketRegistry(),
                this.nettyNetworkClient.networkChannelHandler.call(), connectedAddress, new HostAndPort(ctx.channel().localAddress()), true);

        this.nettyNetworkClient.channels.add(channel);

        if (this.channel.getHandler() != null)
            this.channel.getHandler().handleChannelInitialize(this.channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
            if (this.channel.getHandler() != null)
                this.channel.getHandler().handleChannelClose(this.channel);

            ctx.channel().close();

            this.nettyNetworkClient.channels.remove(this.channel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException) && !(cause instanceof ClosedChannelException))
            cause.printStackTrace();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
        nettyNetworkClient.taskScheduler.schedule((Callable<Void>) () -> {
            if (channel.getHandler() != null && !channel.getHandler().handlePacketReceive(channel, msg))
                return null;

            channel.getPacketRegistry().handlePacket(channel, msg);
            return null;
        });
    }
}