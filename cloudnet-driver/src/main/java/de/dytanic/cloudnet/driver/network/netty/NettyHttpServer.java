package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class NettyHttpServer extends NettySSLServer implements IHttpServer {

    protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = Maps.newConcurrentHashMap();

    protected final List<HttpHandlerEntry> registeredHandlers = Iterables.newCopyOnWriteArrayList();

    protected final EventLoopGroup bossGroup = NettyUtils.newEventLoopGroup(), workerGroup = NettyUtils.newEventLoopGroup();

    public NettyHttpServer() throws Exception {
        this(null);
    }

    public NettyHttpServer(SSLConfiguration sslConfiguration) throws Exception {
        super(sslConfiguration);

        this.init();
    }

    /*= ---------------------------------------------------------------------------------- =*/

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
        Validate.checkNotNull(hostAndPort);
        Validate.checkNotNull(hostAndPort.getHost());

        if (!channelFutures.containsKey(hostAndPort.getPort()))
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
            } catch (InterruptedException e) {
                e.printStackTrace();
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
        Validate.checkNotNull(path);
        Validate.checkNotNull(handlers);

        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && !path.equals("/")) path = path.substring(0, path.length() - 1);

        for (IHttpHandler httpHandler : handlers)
            if (httpHandler != null) {
                boolean value = true;

                for (HttpHandlerEntry registeredHandler : this.registeredHandlers)
                    if (registeredHandler.path.equals(path) && registeredHandler.httpHandler.getClass().equals(httpHandler.getClass())) {
                        value = false;
                        break;
                    }

                if (value) this.registeredHandlers.add(new HttpHandlerEntry(path, httpHandler, port, priority));
            }

        return this;
    }

    @Override
    public IHttpServer removeHandler(IHttpHandler handler) {
        Validate.checkNotNull(handler);

        for (HttpHandlerEntry registeredHandler : this.registeredHandlers)
            if (registeredHandler.httpHandler.equals(handler))
                this.registeredHandlers.remove(registeredHandler);

        return this;
    }

    @Override
    public IHttpServer removeHandler(Class<? extends IHttpHandler> handler) {
        Validate.checkNotNull(handler);

        for (HttpHandlerEntry registeredHandler : this.registeredHandlers)
            if (registeredHandler.httpHandler.getClass().equals(handler))
                this.registeredHandlers.remove(registeredHandler);

        return this;
    }

    @Override
    public IHttpServer removeHandler(ClassLoader classLoader) {
        Validate.checkNotNull(classLoader);

        for (HttpHandlerEntry registeredHandler : this.registeredHandlers)
            if (registeredHandler.httpHandler.getClass().getClassLoader().equals(classLoader))
                this.registeredHandlers.remove(registeredHandler);

        return this;
    }

    @Override
    public Collection<IHttpHandler> getHttpHandlers() {
        return Iterables.map(this.registeredHandlers, httpHandlerEntry -> httpHandlerEntry.httpHandler);
    }

    @Override
    public IHttpServer clearHandlers() {
        this.registeredHandlers.clear();
        return this;
    }

    @Override
    public void close() throws Exception {
        for (Pair<HostAndPort, ChannelFuture> entry : this.channelFutures.values())
            entry.getSecond().cancel(true);

        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        this.clearHandlers();
    }

    /*= ---------------------------------------------------------- =*/

    @ToString
    @EqualsAndHashCode
    public class HttpHandlerEntry implements Comparable<HttpHandlerEntry> {

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
        public int compareTo(HttpHandlerEntry httpHandlerEntry) {
            Validate.checkNotNull(httpHandlerEntry);

            return this.priority + httpHandlerEntry.priority;
        }
    }
}