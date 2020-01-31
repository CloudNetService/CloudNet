package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public final class NettyNetworkServer extends NettySSLServer implements INetworkServer {

    protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = Maps.newConcurrentHashMap();

    protected final Collection<INetworkChannel> channels = Iterables.newConcurrentLinkedQueue();

    protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

    protected final EventLoopGroup bossEventLoopGroup = NettyUtils.newEventLoopGroup(), workerEventLoopGroup = NettyUtils.newEventLoopGroup();

    protected final ITaskScheduler taskScheduler;

    protected final boolean taskSchedulerFromConstructor;

    protected final Callable<INetworkChannelHandler> networkChannelHandler;

    public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler) {
        this(networkChannelHandler, null, null);
    }

    public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler, ITaskScheduler taskScheduler) {
        this(networkChannelHandler, null, taskScheduler);
    }

    public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler, SSLConfiguration sslConfiguration, ITaskScheduler taskScheduler) {
        super(sslConfiguration);
        this.networkChannelHandler = networkChannelHandler;
        this.taskSchedulerFromConstructor = taskScheduler != null;
        this.taskScheduler = taskScheduler == null ? new DefaultTaskScheduler(Runtime.getRuntime().availableProcessors()) : taskScheduler;

        try {
            this.init();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean isSslEnabled() {
        return sslContext != null;
    }


    @Override
    public boolean addListener(int port) {
        return this.addListener(new HostAndPort("0.0.0.0", port));
    }

    @Override
    public boolean addListener(@NotNull HostAndPort hostAndPort) {
        Validate.checkNotNull(hostAndPort);
        Validate.checkNotNull(hostAndPort.getHost());

        if (!this.channelFutures.containsKey(hostAndPort.getPort())) {
            try {
                this.channelFutures.put(hostAndPort.getPort(), new Pair<>(hostAndPort, new ServerBootstrap()
                        .group(bossEventLoopGroup, workerEventLoopGroup)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.IP_TOS, 24)
                        .childOption(ChannelOption.AUTO_READ, true)
                        .channel(NettyUtils.getServerSocketChannelClass())
                        .childHandler(new NettyNetworkServerInitializer(this, hostAndPort))
                        .bind(hostAndPort.getHost(), hostAndPort.getPort())
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                        .sync()
                        .channel()
                        .closeFuture()));

                return true;
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void close() {
        taskScheduler.shutdown();
        this.closeChannels();

        for (Pair<HostAndPort, ChannelFuture> entry : this.channelFutures.values()) {
            entry.getSecond().cancel(true);
        }

        this.bossEventLoopGroup.shutdownGracefully();
        this.workerEventLoopGroup.shutdownGracefully();
    }

    public Collection<INetworkChannel> getChannels() {
        return Collections.unmodifiableCollection(this.channels);
    }

    @Override
    public void closeChannels() {
        for (INetworkChannel channel : this.channels) {
            try {
                channel.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        this.channels.clear();
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Validate.checkNotNull(packet);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packet);
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket... packets) {
        Validate.checkNotNull(packets);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packets);
        }
    }

    public IPacketListenerRegistry getPacketRegistry() {
        return this.packetRegistry;
    }
}