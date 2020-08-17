package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketSendEvent;
import de.dytanic.cloudnet.driver.network.DefaultNetworkChannel;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.NotNull;

final class NettyNetworkChannel extends DefaultNetworkChannel implements INetworkChannel {

    private final Channel channel;

    public NettyNetworkChannel(Channel channel, IPacketListenerRegistry packetRegistry, INetworkChannelHandler handler,
                               HostAndPort serverAddress, HostAndPort clientAddress, boolean clientProvidedChannel) {
        super(packetRegistry, serverAddress, clientAddress, clientProvidedChannel, handler);
        this.channel = channel;
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        if (this.channel.eventLoop().inEventLoop()) {
            this.writePacket(packet);
        } else {
            this.channel.eventLoop().execute(() -> this.writePacket(packet));
        }
    }

    @Override
    public void sendPacketSync(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        ChannelFuture future = this.writePacket(packet);
        if (future != null) {
            future.syncUninterruptibly();
        }
    }

    @Override
    public boolean isWriteable() {
        return this.channel.isWritable();
    }

    @Override
    public boolean isActive() {
        return this.channel.isActive();
    }

    private ChannelFuture writePacket(IPacket packet) {
        NetworkChannelPacketSendEvent event = new NetworkChannelPacketSendEvent(this, packet);

        CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> cloudNetDriver.getEventManager().callEvent(event));

        if (!event.isCancelled()) {
            if (packet.isShowDebug()) {
                CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                    if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                        cloudNetDriver.getLogger().debug(
                                String.format(
                                        "Sending packet to %s on channel %d with id %s, header=%s;body=%d",
                                        this.getClientAddress().toString(),
                                        packet.getChannel(),
                                        packet.getUniqueId().toString(),
                                        packet.getHeader().toJson(),
                                        packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
                                )
                        );
                    }
                });
            }

            return this.channel.writeAndFlush(packet);
        }

        return null;
    }

    @Override
    public void close() {
        this.channel.close();
    }

    public Channel getChannel() {
        return this.channel;
    }

}