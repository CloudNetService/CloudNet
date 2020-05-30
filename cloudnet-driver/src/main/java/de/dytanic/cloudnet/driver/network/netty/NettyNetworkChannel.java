package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketSendEvent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.protocol.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

final class NettyNetworkChannel implements INetworkChannel {

    private static final Callable<Void> EMPTY_TASK = () -> null;

    private static final AtomicLong CHANNEL_ID_COUNTER = new AtomicLong();

    private final long channelId = CHANNEL_ID_COUNTER.addAndGet(1);


    private final Channel channel;

    private final IPacketListenerRegistry packetRegistry;

    private final HostAndPort serverAddress, clientAddress;

    private final boolean clientProvidedChannel;

    private INetworkChannelHandler handler;

    public NettyNetworkChannel(Channel channel, IPacketListenerRegistry packetRegistry, INetworkChannelHandler handler,
                               HostAndPort serverAddress, HostAndPort clientAddress, boolean clientProvidedChannel) {
        this.channel = channel;
        this.handler = handler;

        this.serverAddress = serverAddress;
        this.clientAddress = clientAddress;
        this.clientProvidedChannel = clientProvidedChannel;

        this.packetRegistry = new DefaultPacketListenerRegistry(packetRegistry);
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        if (this.channel.eventLoop().inEventLoop()) {
            this.sendPacket0(packet);
        } else {
            this.channel.eventLoop().execute(() -> this.sendPacket0(packet));
        }
    }

    private void sendPacket0(IPacket packet) {
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
                                        packet.getBody() != null ? packet.getBody().readableBytes() : 0
                                )
                        );
                    }
                });
            }

            this.channel.writeAndFlush(packet, this.channel.voidPromise());
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (IPacket packet : packets) {
            this.sendPacket(packet);
        }
    }

    @Override
    public void close() {
        this.channel.close();
    }

    public long getChannelId() {
        return this.channelId;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public IPacketListenerRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    public HostAndPort getServerAddress() {
        return this.serverAddress;
    }

    public HostAndPort getClientAddress() {
        return this.clientAddress;
    }

    public boolean isClientProvidedChannel() {
        return this.clientProvidedChannel;
    }

    public INetworkChannelHandler getHandler() {
        return this.handler;
    }

    public void setHandler(INetworkChannelHandler handler) {
        this.handler = handler;
    }
}