package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.protocol.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NettyNetworkClient implements INetworkClient {

    private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;

    protected final Collection<INetworkChannel> channels = new ConcurrentLinkedQueue<>();

    protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

    protected final EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();

    protected final Callable<INetworkChannelHandler> networkChannelHandler;

    protected final ITaskScheduler taskScheduler;

    protected final boolean taskSchedulerFromConstructor;

    protected final SSLConfiguration sslConfiguration;

    protected SslContext sslContext;

    protected long connectedTime;

    public NettyNetworkClient(Callable<INetworkChannelHandler> networkChannelHandler) {
        this(networkChannelHandler, null, null);
    }

    public NettyNetworkClient(Callable<INetworkChannelHandler> networkChannelHandler, SSLConfiguration sslConfiguration, ITaskScheduler taskScheduler) {
        this.networkChannelHandler = networkChannelHandler;
        this.sslConfiguration = sslConfiguration;

        this.taskSchedulerFromConstructor = taskScheduler != null;
        this.taskScheduler = taskScheduler == null ? new DefaultTaskScheduler(Runtime.getRuntime().availableProcessors()) : taskScheduler;

        try {
            this.init();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void init() throws Exception {
        if (this.sslConfiguration != null) {
            if (this.sslConfiguration.getCertificatePath() != null &&
                    this.sslConfiguration.getPrivateKeyPath() != null) {
                SslContextBuilder builder = SslContextBuilder.forClient();

                if (this.sslConfiguration.getTrustCertificatePath() != null) {
                    builder.trustManager(this.sslConfiguration.getTrustCertificatePath());
                } else {
                    builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                this.sslContext = builder
                        .keyManager(this.sslConfiguration.getCertificatePath(), this.sslConfiguration.getPrivateKeyPath())
                        .clientAuth(this.sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
                        .build();
            } else {
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                this.sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .keyManager(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                        .build();
            }
        }
    }

    @Override
    public boolean isSslEnabled() {
        return this.sslContext != null;
    }


    @Override
    public boolean connect(@NotNull HostAndPort hostAndPort) {
        Preconditions.checkNotNull(hostAndPort);
        Preconditions.checkNotNull(hostAndPort.getHost());

        try {
            new Bootstrap()
                    .group(this.eventLoopGroup)
                    .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.IP_TOS, 24)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS)
                    .channel(NettyUtils.getSocketChannelClass())
                    .handler(new NettyNetworkClientInitializer(this, hostAndPort, () -> this.connectedTime = System.currentTimeMillis()))
                    .connect(hostAndPort.getHost(), hostAndPort.getPort())
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                    .sync()
                    .channel();

            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public void close() {
        this.taskScheduler.shutdown();
        this.closeChannels();
        this.eventLoopGroup.shutdownGracefully();
    }

    @Override
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
    public long getConnectedTime() {
        return this.connectedTime;
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packet);
        }
    }

    @Override
    public void sendPacketSync(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacketSync(packet);
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packets);
        }
    }

    @Override
    public void sendPacketSync(@NotNull IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacketSync(packets);
        }
    }

    public IPacketListenerRegistry getPacketRegistry() {
        return this.packetRegistry;
    }
}