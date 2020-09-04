package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketSendEvent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.network.protocol.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class NettyNetworkChannel implements INetworkChannel {

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
    public ITask<IPacket> sendQueryAsync(@NotNull IPacket packet) {
        CompletableTask<IPacket> task = new CompletableTask<>();
        InternalSyncPacketChannel.registerQueryHandler(packet.getUniqueId(), task::complete);
        this.sendPacket(packet);
        return task;
    }

    @Override
    public IPacket sendQuery(@NotNull IPacket packet) {
        return this.sendQueryAsync(packet).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public boolean sendChunkedPackets(@NotNull JsonDocument header, @NotNull InputStream inputStream, int channel) throws IOException {
        return ChunkedPacketBuilder.newBuilder(channel, inputStream)
                .header(header)
                .target(this)
                .complete()
                .isSuccess();
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