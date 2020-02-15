package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.http.*;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

final class NettyHttpServerContext implements IHttpContext {

    protected final Collection<HttpCookie> cookies = new ArrayList<>();

    protected final Channel nettyChannel;

    protected final NettyHttpServer nettyHttpServer;

    protected final NettyHttpChannel channel;

    protected final HttpRequest httpRequest;

    protected final NettyHttpServerRequest httpServerRequest;

    protected final NettyHttpServerResponse httpServerResponse;

    protected volatile boolean closeAfter = true, cancelNext = false, cancelSendResponse = false;

    protected volatile NettyWebSocketServerChannel webSocketServerChannel;

    protected IHttpHandler lastHandler;

    public NettyHttpServerContext(NettyHttpServer nettyHttpServer, NettyHttpChannel channel, URI uri, Map<String, String> pathParameters, HttpRequest httpRequest) {
        this.nettyHttpServer = nettyHttpServer;
        this.channel = channel;
        this.httpRequest = httpRequest;
        this.nettyChannel = channel.getChannel();

        this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri);
        this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

        if (this.httpRequest.headers().contains("Cookie")) {
            this.cookies.addAll(ServerCookieDecoder.LAX.decode(this.httpRequest.headers().get("Cookie")).stream().map(cookie -> new HttpCookie(
                    cookie.name(),
                    cookie.value(),
                    cookie.domain(),
                    cookie.path(),
                    cookie.maxAge()
            )).collect(Collectors.toList()));
        }

        this.updateHeaderResponse();
    }

    @Override
    public IWebSocketChannel upgrade() {
        if (webSocketServerChannel == null) {
            cancelSendResponse = true;
            WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(
                    httpRequest.uri(),
                    null,
                    false
            );

            nettyChannel.pipeline().remove("http-server-handler");

            WebSocketServerHandshaker webSocketServerHandshaker = webSocketServerHandshakerFactory.newHandshaker(httpRequest);
            webSocketServerHandshaker.handshake(nettyChannel, httpRequest);

            webSocketServerChannel = new NettyWebSocketServerChannel(channel, nettyChannel, webSocketServerHandshaker);
            nettyChannel.pipeline().addLast("websocket-server-channel-handler", new NettyWebSocketServerChannelHandler(webSocketServerChannel));

            closeAfter(false);
        }

        return webSocketServerChannel;
    }

    @Override
    public IWebSocketChannel webSocketChanel() {
        return webSocketServerChannel;
    }

    @Override
    public IHttpChannel channel() {
        return this.channel;
    }

    @Override
    public IHttpRequest request() {
        return this.httpServerRequest;
    }

    @Override
    public IHttpResponse response() {
        return this.httpServerResponse;
    }

    @Override
    public boolean cancelNext() {
        return this.cancelNext = true;
    }

    @Override
    public IHttpHandler peekLast() {
        return this.lastHandler;
    }

    @Override
    public IHttpComponent<IHttpServer> component() {
        return this.nettyHttpServer;
    }

    @Override
    public IHttpContext closeAfter(boolean value) {
        this.closeAfter = value;
        return this;
    }

    @Override
    public boolean closeAfter() {
        return closeAfter;
    }

    @Override
    public HttpCookie cookie(String name) {
        Preconditions.checkNotNull(name);

        return this.cookies.stream().filter(httpCookie -> httpCookie.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public Collection<HttpCookie> cookies() {
        return this.cookies;
    }

    @Override
    public boolean hasCookie(String name) {
        Preconditions.checkNotNull(name);

        return this.cookies.stream().anyMatch(httpCookie -> httpCookie.getName().equalsIgnoreCase(name));
    }

    @Override
    public IHttpContext setCookies(Collection<HttpCookie> cookies) {
        Preconditions.checkNotNull(cookies);

        this.cookies.clear();
        this.cookies.addAll(cookies);
        this.updateHeaderResponse();

        return this;
    }

    @Override
    public IHttpContext addCookie(HttpCookie httpCookie) {
        Preconditions.checkNotNull(httpCookie);

        HttpCookie cookie = cookie(httpCookie.getName());

        if (cookie != null) {
            this.removeCookie(cookie.getName());
        }
        this.cookies.add(httpCookie);
        this.updateHeaderResponse();

        return this;
    }

    @Override
    public IHttpContext removeCookie(String name) {
        Preconditions.checkNotNull(name);

        HttpCookie cookie = cookie(name);
        if (cookie != null) {
            cookie.setMaxAge(-1);
        }

        this.updateHeaderResponse();
        return this;
    }

    @Override
    public IHttpContext clearCookies() {
        this.cookies.clear();
        this.updateHeaderResponse();
        return this;
    }

    private void updateHeaderResponse() {
        if (cookies.isEmpty()) {
            this.httpServerResponse.httpResponse.headers().remove("Set-Cookie");
        } else {
            this.httpServerResponse.httpResponse.headers()
                    .set("Set-Cookie", ServerCookieEncoder.LAX.encode(this.cookies.stream().map(httpCookie -> {
                        Cookie cookie = new DefaultCookie(httpCookie.getName(), httpCookie.getValue());
                        cookie.setDomain(httpCookie.getDomain());
                        cookie.setMaxAge(httpCookie.getMaxAge());
                        cookie.setPath(httpCookie.getPath());

                        return cookie;
                    }).collect(Collectors.toList())));
        }
    }

    public void setLastHandler(IHttpHandler lastHandler) {
        this.lastHandler = lastHandler;
    }
}