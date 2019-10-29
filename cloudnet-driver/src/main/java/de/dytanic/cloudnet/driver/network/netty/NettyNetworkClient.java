package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

public final class NettyNetworkClient implements INetworkClient {

    protected final Collection<INetworkChannel> channels = Iterables.newConcurrentLinkedQueue();

    protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

    protected final EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();

    protected final Callable<INetworkChannelHandler> networkChannelHandler;

    protected final ITaskScheduler taskScheduler;

    protected final boolean taskSchedulerFromConstructor;

    protected final SSLConfiguration sslConfiguration;

    protected SslContext sslContext;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        if (sslConfiguration != null) {
            if (sslConfiguration.getCertificatePath() != null &&
                    sslConfiguration.getPrivateKeyPath() != null) {
                SslContextBuilder builder = SslContextBuilder.forClient();

                if (sslConfiguration.getTrustCertificatePath() != null) {
                    builder.trustManager(sslConfiguration.getTrustCertificatePath());
                } else {
                    builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                this.sslContext = builder
                        .keyManager(sslConfiguration.getCertificatePath(), sslConfiguration.getPrivateKeyPath())
                        .clientAuth(sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
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
        return sslContext != null;
    }


    @Override
    public boolean connect(HostAndPort hostAndPort) {
        Validate.checkNotNull(hostAndPort);
        Validate.checkNotNull(hostAndPort.getHost());

        try {
            new Bootstrap()
                    .group(eventLoopGroup)
                    .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.IP_TOS, 24)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2500)
                    .channel(NettyUtils.getSocketChannelClass())
                    .handler(new NettyNetworkClientInitializer(this, hostAndPort))
                    .connect(hostAndPort.getHost(), hostAndPort.getPort())
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                    .sync()
                    .channel();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void close() {
        taskScheduler.shutdown();
        this.closeChannels();
        eventLoopGroup.shutdownGracefully();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.channels.clear();
    }

    @Override
    public void sendPacket(IPacket packet) {
        Validate.checkNotNull(packet);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packet);
        }
    }

    @Override
    public void sendPacket(IPacket... packets) {
        Validate.checkNotNull(packets);

        for (INetworkChannel channel : this.channels) {
            channel.sendPacket(packets);
        }
    }

    public IPacketListenerRegistry getPacketRegistry() {
        return this.packetRegistry;
    }
}