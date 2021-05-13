package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;

@ApiStatus.Internal
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<Packet> {

    protected NettyNetworkChannel channel;

    protected abstract Collection<INetworkChannel> getChannels();

    protected abstract Executor getPacketDispatcher();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
            if (this.channel.getHandler() != null) {
                this.channel.getHandler().handleChannelClose(this.channel);
            }

            ctx.channel().close();
            this.getChannels().remove(this.channel);
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
        this.getPacketDispatcher().execute(() -> {
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
