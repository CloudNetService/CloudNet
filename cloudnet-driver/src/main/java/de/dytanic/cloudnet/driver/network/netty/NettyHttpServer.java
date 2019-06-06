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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        return Iterables.map(this.registeredHandlers, new Function<HttpHandlerEntry, IHttpHandler>() {
            @Override
            public IHttpHandler apply(HttpHandlerEntry httpHandlerEntry) {
                return httpHandlerEntry.httpHandler;
            }
        });
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

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof HttpHandlerEntry)) return false;
            final HttpHandlerEntry other = (HttpHandlerEntry) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$path = this.path;
            final Object other$path = other.path;
            if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
            final Object this$httpHandler = this.httpHandler;
            final Object other$httpHandler = other.httpHandler;
            if (this$httpHandler == null ? other$httpHandler != null : !this$httpHandler.equals(other$httpHandler))
                return false;
            final Object this$port = this.port;
            final Object other$port = other.port;
            if (this$port == null ? other$port != null : !this$port.equals(other$port)) return false;
            if (this.priority != other.priority) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof HttpHandlerEntry;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $path = this.path;
            result = result * PRIME + ($path == null ? 43 : $path.hashCode());
            final Object $httpHandler = this.httpHandler;
            result = result * PRIME + ($httpHandler == null ? 43 : $httpHandler.hashCode());
            final Object $port = this.port;
            result = result * PRIME + ($port == null ? 43 : $port.hashCode());
            result = result * PRIME + this.priority;
            return result;
        }

        public String toString() {
            return "NettyHttpServer.HttpHandlerEntry(path=" + this.path + ", httpHandler=" + this.httpHandler + ", port=" + this.port + ", priority=" + this.priority + ")";
        }
    }
}