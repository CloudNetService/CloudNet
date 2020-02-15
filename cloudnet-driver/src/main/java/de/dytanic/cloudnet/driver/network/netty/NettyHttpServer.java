package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class NettyHttpServer extends NettySSLServer implements IHttpServer {

    protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = new ConcurrentHashMap<>();

    protected final List<HttpHandlerEntry> registeredHandlers = new CopyOnWriteArrayList<>();

    protected final EventLoopGroup bossGroup = NettyUtils.newEventLoopGroup(), workerGroup = NettyUtils.newEventLoopGroup();

    public NettyHttpServer() throws Exception {
        this(null);
    }

    public NettyHttpServer(SSLConfiguration sslConfiguration) throws Exception {
        super(sslConfiguration);

        this.init();
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
    public boolean addListener(HostAndPort hostAndPort) {
        Preconditions.checkNotNull(hostAndPort);
        Preconditions.checkNotNull(hostAndPort.getHost());

        if (!channelFutures.containsKey(hostAndPort.getPort())) {
            try {
                this.channelFutures.put(hostAndPort.getPort(), new Pair<>(hostAndPort, new ServerBootstrap()
                        .group(bossGroup, workerGroup)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.IP_TOS, 24)
                        .childOption(ChannelOption.AUTO_READ, true)
                        .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                        .channel(NettyUtils.getServerSocketChannelClass())
                        .childHandler(new NettyHttpServerInitializer(this, hostAndPort))
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
    public IHttpServer registerHandler(String path, IHttpHandler... handlers) {
        return this.registerHandler(path, IHttpHandler.PRIORITY_NORMAL, handlers);
    }

    @Override
    public IHttpServer registerHandler(String path, int priority, IHttpHandler... handlers) {
        return this.registerHandler(path, null, priority, handlers);
    }

    @Override
    public IHttpServer registerHandler(String path, Integer port, int priority, IHttpHandler... handlers) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(handlers);

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && !path.equals("/")) {
            path = path.substring(0, path.length() - 1);
        }

        for (IHttpHandler httpHandler : handlers) {
            if (httpHandler != null) {
                boolean value = true;

                for (HttpHandlerEntry registeredHandler : this.registeredHandlers) {
                    if (registeredHandler.path.equals(path) && registeredHandler.httpHandler.getClass().equals(httpHandler.getClass())) {
                        value = false;
                        break;
                    }
                }

                if (value) {
                    this.registeredHandlers.add(new HttpHandlerEntry(path, httpHandler, port, priority));
                }
            }
        }

        return this;
    }

    @Override
    public IHttpServer removeHandler(IHttpHandler handler) {
        Preconditions.checkNotNull(handler);

        this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.equals(handler));

        return this;
    }

    @Override
    public IHttpServer removeHandler(Class<? extends IHttpHandler> handler) {
        Preconditions.checkNotNull(handler);

        this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.getClass().equals(handler));

        return this;
    }

    @Override
    public IHttpServer removeHandler(ClassLoader classLoader) {
        Preconditions.checkNotNull(classLoader);

        this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.getClass().getClassLoader().equals(classLoader));

        return this;
    }

    @Override
    public Collection<IHttpHandler> getHttpHandlers() {
        return this.registeredHandlers.stream().map(httpHandlerEntry -> httpHandlerEntry.httpHandler).collect(Collectors.toList());
    }

    @Override
    public IHttpServer clearHandlers() {
        this.registeredHandlers.clear();
        return this;
    }

    @Override
    public void close() {
        for (Pair<HostAndPort, ChannelFuture> entry : this.channelFutures.values()) {
            entry.getSecond().cancel(true);
        }

        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        this.clearHandlers();
    }


    @ToString
    @EqualsAndHashCode
    public static class HttpHandlerEntry implements Comparable<HttpHandlerEntry> {

        public final String path;

        public final IHttpHandler httpHandler;

        public final Integer port;

        public final int priority;

        public HttpHandlerEntry(String path, IHttpHandler httpHandler, Integer port, int priority) {
            this.path = path;
            this.httpHandler = httpHandler;
            this.port = port;
            this.priority = priority;
        }

        @Override
        public int compareTo(@NotNull HttpHandlerEntry httpHandlerEntry) {
            Preconditions.checkNotNull(httpHandlerEntry);

            return this.priority + httpHandlerEntry.priority;
        }
    }
}